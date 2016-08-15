---
title: autoscript
---
## 批次程式佈署
現今資訊系統架構主流是分散式系統，因此系統管理者常要管理成千數萬的資訊設備，若這些資訊設備遇到更新軟體的需求，目前主流的作法是預先在系統中安裝agent，中央的系統控管伺服器派送工作到agent執行，若不採用這種方式來維運資訊系統，可由管理者在其中一台主機，執行預先定義好的執行腳本，遠端控制這些設備執行系統更新，也可達到節省人力時間的效果

## 批次程式佈署技術
這種執行腳本遠端控制的技術很多，常見的終端機軟體putty, plink, telnet都支援這樣的技術，但通常都有一些使用上的限制(ex: su指令)，但expect克服了這些困難，他導向了執行主機的stdout和stdin，因此可以最完整執行使用者定義的所有動作
expect在windows環境上的實作，我找到四個
activestate expect和chaffee expect需另外再安裝tcl script engine，而dejagnu則是以cygwin做為執行環境，這三個需再另外安裝其他軟體，因此選擇expect .net，不需再另外安裝軟體就可開發使用
expect.net用了.net技術實作，對微軟系統的使用者來說，使用門檻最低

## 批次程式佈署需求
實作此系統需求如下：
1. 使用plink透過ssh連線到遠端linux主機控制系統
2. 使用psftp透過sftp協定傳遞檔案到遠端
3. 將usb mount起來
4. kill掉某個process
5. 處理過程發生問題，需通知管理者

## 系統實作
### expect .net api introduce
expect .net 常用api如下
1. new ProcessSpawnable(execute_command, command_args)
   建立執行命令
2. Session spawn = Expect.Spawn(ISpawnable);
   執行ISPawnable命令
3. spawn.Expect("Password:", (s) => Console.WriteLine("found: " + s));
   等待遠端的回應字串，若等到回應，將執行第二個參數定義的anonymous function，若等待時間太久超過timeout時間，則會發出exception<br />
4. spawn.Timeout = 30000;
   設定expect的最長等待時間<br />
5. spawn.send
   送出執行指令到遠端

### 如何讓這些api動起來
以使用plink登入遠端linux主機為例
```cs
//開啟plink程式
Session spawn = Expect.Spawn(new ProcessSpawnable("c:\\plink.exe","-l username -pw password 192.168.13.77 -t"));

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

### 錯誤處理
考量系統管理者使用此工具時，都是針對大量設備執行工作，因此錯誤管理的機制非常重要，若執行的過程中有問題，必須清楚讓管理者清楚哪邊出問題，系統中針對每一台設備執行的過程，都會開啟檔案紀錄，若執行過程發生錯誤，系統會更改檔名，加註err在檔名中，管理者就可以清楚知道哪台設備更新過程出問題，進入處理

### 系統參數
第一個參數是欲執行script檔路徑
第二個參數是script檔中的變數的值
expect script_file ip

### 指令實作
為了可以讓系統管理者隨時定義他想做的工作內容，我將上述expect.net api包裝成一種script language，指令說明如下
1. spawn
```bs
///建立一個spawnobject物件，執行execute_command command_args
spawn spawnobject "execute_command","command_args"
```

2. expect
```bs
//執行do_something後，等待終端機回傳內含expect_string字串，並且檢查do_something執行結果，若在timeout時間內等到，則印出字串
spawnobject.expect_with_check expect_string do_something
//執行do_something後，等待終端機回傳內含expect_string字串，等待時間拉長到30s，若在timeout時間內等到，則印出字串，此指令解決ftp檔案上傳，指令回應速度較慢的問題
spawnobject.expect_longtimeout expect_string do_something
//執行do_something後，等待終端機回傳內含expect_string字串，若在timeout時間內等到，則印出字串
spawnobject.expect expect_string do_something
```
3. pid
```bs
//取得processname執行的pid，並將值指定到processobject中
spawnobject.assignpid processobject processname
```

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
```cs
 ((Session)spawn_dictionary[param_arr[0]]).Send("kill " + ((string)spawn_dictionary[op2]) + "\n");
```
4. mount
```bs
mount隨身碟到mountpoint路徑
spawnobject.mountusb usbobject mountpoint
```
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
//對應系統第二個執行參數對應的值
<?ip>
## 物件實作
### dictionary object
dictionary以key, value的形式儲存物件，系統中spawn和assignpid兩個指令，會定義物件，script中的第二個參數物件名字就放在dictionary的key中，對應expect.net物件，儲存在value
```cs
spawn_dictionary.Add(op1, ps_results[ps_results.Length - 2]);
後續自dictionary取用物件
```cs
((Session)spawn_dictionary[param_arr[0]]).Expect("root", (s) => opcode = s);
```
## 應用
將前節expect .net的實作修改為這個script language的語法
```bs
//建立名字s1的spawnobject執行plink
spawn s1 "c:\plink.exe","-l username -pw password  -t"
//建立名字s2的spawnobject執行psftp
spawn s2 "c:\psftp.exe","-l username -pw password "
//在plink上執行su，執行完畢後預期收到"password:"字串
s1.expect "Password:" "su"
s1.expect "#" "1234"
//在plink上執行chmod，並且檢查執行結果，執行完畢後預期收到"#"字串
s1.expect_with_check "#" "chmod 777 /home/guest"
s1.expect_with_check "#" "mkdir /home/usb"
//在plink上執行mount到/home/usb
s1.mountusb osusb /home/usb
s2.expect_with_check ">" "cd /home/guest/"
//在psftp上執行put，執行完畢後預期收到">"字串，考量傳資料會花較長時間，因此執行longtimeout
s2.expect_longtimeout ">" "put c:\test.txt"
//在plink上取得execute_pcs的pid將值指定到ncspid變數
s1.assignpid ncspid execute_pcs
//在plink上取得kill execute_pcs process
s1.kill "#" ncspid
s1.expect_with_check "#" "cp /home/guest/test.txt /home/usb/test/"
s1.expect_with_check "#" "umount /home/usb/"
s1.expect "#" rmdir /home/usb/"
```


## future work
針對各種管理需求，本系統還可以繼續實作各種管理功能，包含getpid可以抓取多個prcess id的功能，這都是後續可以精進的方向