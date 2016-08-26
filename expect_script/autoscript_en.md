---
title: autoscript
---
### 1. Mass program upgrade
It's a hard work for the IT system operators to upgrade program on the equipment without any tools nowaday because of the distributed architecture. Executing a predefined script automatically to update program remotely is one of the common solution to the problem. And that can save much work and time.

### 2. Mass program installation tool kits
Many tools support executing script automatically and controlling the machine remotely, such as putty, plink and telnet. But they have some limitations ex: su command.
"Expect" technique overcomes the problem, it redirects the stdin and stdout to the program which runs like an operator fires the command with terminal
There are four projects implemented with "expect" technique on the windows plateform --- activestate expect、chaffee expect、dejagnu and expect .net. Activestate expect and Chaffee expect must install tcl script engine previously and dejagnu runs on the cygwin. Those three have to install other tools. The last one "expect.net", it's a program library implemented with .net. It's easy to be familiar with it. So I choose it as my tool to solve the mass program upgrade issue.

### 3. Requirement for mass program upgrade tool
The requirement is as following：
1. Use terminal program to control the remote server through ssh
2. Transfer files to the remote server via sftp
3. Mount the USB stick at the remote server
4. Stop the service that are going to upgrade later, and then start service
5. Copy the files which is uploaded at step 2 to the specified path
6. Notify operators when something wrong

### 4. Implementation with expect .net
#### 4-1. Introduce of the expect .net API
Common used expect .net API is as following:
```cs
//1. Instantiate a ISpawnable process
ProcessSpawnable(execute_command, command_args)

//2. Execute the ISPawnable process
Session spawn = Expect.Spawn(ISpawnable);

//3. Wait for the response from remote server. If get the reply, execute the anonymous function defined in the second parameter. Otherwise throw an exception. 
spawn.Expect("Password:", (s) => Console.WriteLine("found: " + s))

//4. Set for the waiting timeout
spawn.Timeout = 30000

//5. Fire the command to the remote server
spawn.send   
```   

#### 4-2. Using these API functions
"Login a remote linux server with plink" case as an example to demo expect .net features.
```cs
//Execute plink
Session spawn = Expect.Spawn(new ProcessSpawnable("c:\\plink.exe","-l username -pw password xx.xx.xx.xx -t"));

//Wait for the remote server reponsing the string containing with "$". If we get the string before timeout, print the string out.
spawn.Expect("$", s => Console.WriteLine("got: " + s));

//Send "su" command to the remote server
spawn.Send("su\n");

//Wait for the remote server reponsing the string containing with"Password:". If we get the string before timeout, print the string out.
spawn.Expect("Password:", (s) => Console.WriteLine("found: " + s));

//Send out the password
spawn.Send("1234\n");

//Wait for the remote server reponsing the string containing with "root". If we get the string before timeout, print the string out.
spawn.Expect("root", (s) => Console.WriteLine("found: " + s));

//Send out the command chmod
spawn.Send("chmod 777 /home/guest\n");
```

#### 4-3. Error management
Considering the job "Mass upgrade", error management is very important. If something is wrong during the upgrade process, the tool has to let user know what and where is wrong easily and clearly. Now I design a ,echanism that the tool keeps the log for every remote server during execution time. The log file name will be replaced with "err" in the tail when something is wrong during the execution time. The operator just checks the filename with "err" and he can know the problem.

#### 4-4. Arguments of the tool
The tool designs two execution arguements. Their meaning is as following:
```bs
expect script_file_path remote_server_ip
```
The first one is the path for the execution script
The second one is the remote server ip. The arguments defines the value of the variable '<?ip>' in the script file. 


#### 4-5. The description of the script in the tool
I develope a script language with expect .net library and operators can define their job by themselves with the script. The format of the script is as following:
```sh
opcode op1 op2
#opcode is the name of the script command
#op1, op2 is the arguments for the opcode. The number of the argument could be one or two.
```
The script command is described below:
1.spawn
```sh
#Instantiate a spawnobject object and execute the arguments "execute_command" and "command_args"
spawn spawnobject "execute_command","command_args"
```

2.expect
```sh
#1. After executing do_something, wait for the string "expect_string" and check the execution result of the do_something.
spawnobject.expect_with_check expect_string do_something

#2. After executing do_something, wait for the string "expect_string" up to 30 seconds. The command can solve the problem that longer time responsing when file uploaded via ftp.
spawnobject.expect_longtimeout expect_string do_something

#3. A basic command for expect that wait for the string "expect_string", after executing do_something
spawnobject.expect expect_string do_something
```
3.Get process pid and kill pid
```sh
#Get the pid of the process which name is processname，and assign the pid to the processobject
spawnobject.assignpid processobject processname
```
How to get pid in the engine
```cs
//grep -v "grep" exclude "grep op2" process pid
 ((Session)spawn_dictionary[param_arr[0]]).Send("ps -aux | grep " + op2 + " | grep -v "grep"\n");

 ((Session)spawn_dictionary[param_arr[0]]).Expect("root", (s) => ps_result = s);
 string[] ps_results = ps_result.Split('\n');

 //Get the last record pid
 writelog(fs, "pid:" + ps_results[ps_results.Length - 2]);

 spawn_dictionary.Add(op1, ps_results[ps_results.Length - 2]);
```
```bs
//kill command must be executed immeduately after assognpid. It can kill the pid stored in processobject
spawnobject.kill processobject
```
The implementation for kill processobject
```cs
 ((Session)spawn_dictionary[param_arr[0]]).Send("kill " + ((string)spawn_dictionary[op2]) + "\n");
```
4.mount
```bs
#mount usb sticks to the mountpoint path
spawnobject.mountusb usbobject mountpoint
```
How does mount script run?
```cs
//Find the smallest disk
 ((Session)spawn_dictionary[param_arr[0]]).Send("lsblk -b | grep ^sd | awk -v min=100000000000000000 '{if(min>$4){min=$4;name=$1}}END {print name}'\n");
 ((Session)spawn_dictionary[param_arr[0]]).Expect("root", (s) => opcode = s);
 //Get the name of the disk
 int indexofsd = opcode.LastIndexOf("sd");
 if (indexofsd < 0)
 {
  error = 1;
  writelog(fs, "error:mount usb error");
 }
 string usbname = opcode.Substring(indexofsd, 3);
 ((Session)spawn_dictionary[param_arr[0]]).Send("mount /dev/" + usbname + "1 " + op2 + "\n");
```
4.assign the remote server ip
```bs
Now  the tool has a varaible --- <?ip>. And user can defined it in the second argument of the command line。
```
#### 4-6. Implementation for the storing the user-defined script object
The program uses .net Dictionary class to store the script object. Dictionary uses key and value pair to store information so the two script commands --- spawn and assignpid, their objectname which is defined in the script is stored in the key part of Dictionary otherwise the object instance is store in the value part.
```cs
//the op1 of the script command is stored in the dictionary
spawn_dictionary.Add(op1, ps_results[ps_results.Length - 2]);
//Get the object from dictionary
((Session)spawn_dictionary[param_arr[0]]).Expect("root", (s) => opcode = s);
```
### 5. The usage of the script
We use the tool to rewrite the sction 4.2 from c# code into this script language
```sh
#Instantiate a spawnobject object which name is s1 and execute plink and the argument for the plink is "-l username -pw password <?ip>  -t". And user can define <?ip> variable in the second argument of the command line.
spawn s1 "c:\plink.exe","-l username -pw password <?ip>  -t"

#Instantiate a spawnobject object which name is s1 and execute psftp. 
spawn s2 "c:\psftp.exe","-l username -pw password "

#在plink上執行su，執行完畢後預期收到"password:"字串
s1.expect "Password:" "su"
s1.expect "#" "1234"

#在plink上執行chmod，並且檢查執行結果，執行完畢後預期收到"#"字串
s1.expect_with_check "#" "chmod 777 /home/guest"
s1.expect_with_check "#" "mkdir /home/usb"

#在plink上執行mount到/home/usb
s1.mountusb osusb /home/usb
s2.expect_with_check ">" "cd /home/guest/"

#在psftp上執行put，執行完畢後預期收到">"字串，考量傳資料會花較長時間，因此執行longtimeout
s2.expect_longtimeout ">" "put c:\test.txt"

#在plink上取得execute_pcs的pid將值指定到ncspid變數
s1.assignpid ncspid execute_pcs

#在plink上取得kill execute_pcs process
s1.kill "#" ncspid

s1.expect_with_check "#" "cp /home/guest/test.txt /home/usb/test/"
s1.expect_with_check "#" "umount /home/usb/"
s1.expect "#" rmdir /home/usb/"
```


### 6. Future work
針對各種管理需求，本系統還可以繼續實作各種管理功能，包含getpid可以抓取多個prcess id的功能，這都是後續可以精進的方向