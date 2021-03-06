---
title: autoscript
---
### 1. 批次軟體升級佈署
現今資訊系統架構大多是分散式系統，因此系統管理者需要管理成百上千的資訊設備，若這些資訊設備遇到升級軟體的需求，常見的作法之一是由管理者在其中一台主機，自動執行預先定義好的腳本，遠端控制這些設備執行系統更新，可達到節省人力時間的效果。

### 2. 批次軟體升級佈署技術
自動執行腳本遠端控制的技術很多，常見的終端機軟體putty, plink, telnet都支援這樣的技術，但通常都有一些使用上的限制(ex: su指令)，expect技術克服了這些困難，他導向了執行主機的stdout和stdin到程式中，讓程式運行時如同系統管理者正在終端機前下指令。

expect在windows環境上的實作，主要有四個專案：activestate expect、chaffee expect、dejagnu和expect .net。
activestate expect和chaffee expect需另外再安裝tcl script engine；dejagnu則是以cygwin做為執行環境，由於這三個工具需再另外安裝其他軟體，因此選擇expect .net，expect.net是一個.net技術實作的函式庫，對熟悉微軟系統的使用者來說，使用門檻最低。

這篇文章將使用expect.net開發一個可提供批次軟體升級佈署的script engine，提供使用者自定執行腳本執行大量遠端佈署的工作。

### 3. script engine需求
script engine需求如下：
1. 使用終端機軟體透過ssh協定連線到遠端linux主機控制系統
2. 使用sftp協定傳遞檔案到遠端
3. 掛載遠端伺服器的USB隨身碟
4. 停止稍後要更新的服務
5. 需求2的檔案複製到正確的本機路徑和隨身碟空間，並啟動服務
6. 處理過程若發生問題，需通知管理者

### 4. 實作script engine
#### 4-1. expect .net API介紹
expect .net api常用功能如下
```cs
//1. 建立執行命令物件
ProcessSpawnable(execute_command, command_args)

//2. 執行ISPawnable命令
Session spawn = Expect.Spawn(ISpawnable);

//3. 等待遠端的回應字串，若等到回應，將執行第二個參數定義的anonymous function，若等待時間太久超過timeout時間，發出exception
spawn.Expect("Password:", (s) => Console.WriteLine("found: " + s))

//4. 設定expect的最長等待時間
spawn.Timeout = 30000

//5. 送出執行指令到遠端
spawn.send   
```   

#### 4-2. 使用這些api
以使用plink登入遠端linux主機為例，以下展示使用expect.net實作的程式碼
```cs
//開啟plink程式
Session spawn = Expect.Spawn(new ProcessSpawnable("c:\\plink.exe","-l username -pw password xx.xx.xx.xx -t"));

//等待終端機回傳內含"$"字串，若在timeout時間內等到，則印出字串
spawn.Expect("$", s => Console.WriteLine("got: " + s));

//送出su指令
spawn.Send("su\n");

//等待終端機回傳內含"Password:"字串，若在timeout時間內等到，則印出字串
spawn.Expect("Password:", (s) => Console.WriteLine("found: " + s));

//送出密碼
spawn.Send("1234\n");

//等待終端機回傳內含"root"字串，若在timeout時間內等到，則印出字串
spawn.Expect("root", (s) => Console.WriteLine("found: " + s));

//送出指令
spawn.Send("chmod 777 /home/guest\n");
```
#### 4-4. 錯誤處理
考量系統管理者使用此工具時，大多針對大量設備執行工作，因此錯誤管理的機制非常重要，若執行的過程中有問題，必須清楚讓管理者清楚哪邊出問題，script engine需針對每一台設備執行的過程，開啟檔案紀錄，若執行過程發生錯誤，engine會更改檔名，加註err在檔名中，管理者就可以清楚知道哪台設備更新過程出問題，進入處理。

#### 4-5. engine執行參數
系統設計兩個執行參數，參數說明如下：
```bs
expect script_file_path remote_server_ip
```
第一個參數是欲執行script檔路徑
第二個參數是script檔中的變數的值


#### 4-6. 指令實作
script engine的指令的基本格式：
```sh
opcode op1 op2
#opcode是命令名稱，除了spawn命令外，其他命令須帶入spawnobject，請參考spawn指令說明
#op1, op2是opcode命令的參數，依照opcode不同，指令參數可能有一個或兩個
```
指令說明如下
1.spawn
```sh
#建立一個spawnobject物件，執行execute_command command_args
spawn spawnobject "execute_command","command_args"
```
產生的spawnobject代表一個主控台視窗，後續可使用此object執行expect或是mount命令，相當於開啟主控台後，在該主控台視窗終執行程式，在script為了辨別是在那個視窗執行，執行expect或是send功能時，前面須帶入spawnobject名稱，ex:spawnobject.expect。
spawnobject的實作採用.net Dictionary物件，Dictionary物件以key, value的形式儲存物件，engine中spawn和assignpid兩個指令為物件定義指令，script中的第二個參數物件名字就放在dictionary的key中，對應expect.net物件，儲存在value
```cs
//將指令的op1存入dictionary中
spawn_dictionary.Add(op1, ps_results[ps_results.Length - 2]);
//自dictionary取用物件
((Session)spawn_dictionary[param_arr[0]]).Expect("root", (s) => opcode = s);
```


2.expect
expect指令須帶入要執行的spawnobject，共有三個指令，說明如下：
```sh
#1. 執行do_something後，等待終端機回傳內含expect_string字串，並且檢查do_something執行結果
spawnobject.expect_with_check expect_string do_something

#2. 執行do_something後，等待終端機回傳內含expect_string字串，等待時間拉長到30s，若在timeout時間內等到，則印出字串，此指令解決ftp檔案上傳，指令回應速度較慢的問題
spawnobject.expect_longtimeout expect_string do_something

#3. 執行do_something後，等待終端機回傳內含expect_string字串，若在timeout時間內等到，則印出字串
spawnobject.expect expect_string do_something
```
3.取得pid，kill pid
```sh
#取得processname執行的pid，並將值指定到processobject中
spawnobject.assignpid processobject processname
```
取得pid的實作
```cs
//grep -v "grep" 排除掉grep op2的pid
 ((Session)spawn_dictionary[param_arr[0]]).Send("ps -aux | grep " + op2 + " | grep -v "grep"\n");

 ((Session)spawn_dictionary[param_arr[0]]).Expect("root", (s) => ps_result = s);
 string[] ps_results = ps_result.Split('\n');

 //取得最後一筆pid(若該process有多個pid，只能取一個)
 writelog(fs, "pid:" + ps_results[ps_results.Length - 2]);

 spawn_dictionary.Add(op1, ps_results[ps_results.Length - 2]);
```
```bs
//kill指令需緊跟著assignpid後，殺掉processobject中的pid
spawnobject.kill processobject
```
kill process的實作
```cs
 ((Session)spawn_dictionary[param_arr[0]]).Send("kill " + ((string)spawn_dictionary[op2]) + "\n");
```
4. mount
```bs
#掛載隨身碟到mountpoint路徑
spawnobject.mountusb usbobject mountpoint
```
mount指令實作
假設USB隨身碟為容量最小的磁碟機，目前的實作是依據磁碟空間來搜尋
```cs
//找出容量最小的磁碟
 ((Session)spawn_dictionary[param_arr[0]]).Send("lsblk -b | grep ^sd | awk -v min=100000000000000000 '{if(min>$4){min=$4;name=$1}}END {print name}'\n");
 ((Session)spawn_dictionary[param_arr[0]]).Expect("root", (s) => opcode = s);
 //取出磁碟的dev名稱
 int indexofsd = opcode.LastIndexOf("sd");
 if (indexofsd < 0)
 {
  error = 1;
  writelog(fs, "error:mount usb error");
 }
 string usbname = opcode.Substring(indexofsd, 3);
 ((Session)spawn_dictionary[param_arr[0]]).Send("mount /dev/" + usbname + "1 " + op2 + "\n");
```
4.遠端主機IP變數
```bs
Engine目前設計<?ip>變數，使用者可在script file中使用，執行時可自主控台指令的第二個參數指定其值。
```
### 5. 應用
將4-2節expect .net的實作修改為這個script engine的語法
```sh
#建立名字s1的spawnobject執行plink，plink執行參數定義"-l username -pw password <?ip> -t"，其中主控台指令的第二個參數指定的值，將會設定到<?ip>變數
spawn s1 "c:\plink.exe","-l username -pw password <?ip> -t"

#建立名字s2的spawnobject執行psftp
spawn s2 "c:\psftp.exe","-l username -pw password <?ip>"

#在plink上執行su，執行完畢後預期收到"password:"字串，再輸入密碼1234
s1.expect "Password:" "su"
s1.expect "#" "1234"

#在plink上執行chmod，並且檢查執行結果，執行完畢後預期收到"#"
s1.expect_with_check "#" "chmod 777 /home/guest"
s1.expect_with_check "#" "mkdir /home/usb"

#在plink上執行mount到/home/usb
s1.mountusb osusb /home/usb

#在psftp上執行put，執行完畢後預期收到">"字串，考量傳資料會花較長時間，採用expect_longtimeout
s2.expect_with_check ">" "cd /home/guest/"
s2.expect_longtimeout ">" "put c:\test.txt"

#在plink上取得execute_pcs的pid將值指定到ncspid變數
s1.assignpid ncspid execute_pcs

#在plink上取得kill execute_pcs process
s1.kill "#" ncspid

s1.expect_with_check "#" "cp /home/guest/test.txt /home/usb/test/"
s1.expect_with_check "#" "umount /home/usb/"
s1.expect "#" rmdir /home/usb/"
```


### 6. future work
針對各種管理需求，本系統還可以繼續實作各種管理功能，包含getpid可以抓取多個prcess id的功能、執行失敗的還原和掛載USB隨身碟，這都是後續可以精進的方向