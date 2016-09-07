---
title: autoscript
---
### 1. Mass program upgrade
It's a hard work for the IT system operators to upgrade program without any tools nowaday because of the distributed architecture. Executing a predefined script automatically to update program remotely is one of the common solution to the problem. And that can save much work and time.

### 2. Mass program installation tool kits
Many tools support executing script automatically and controlling the machine remotely, such as putty, plink and telnet. But they have some limitations ex: su command.
"Expect" technique overcomes the problem and it redirects the stdin and stdout to the program which runs like an operator executes the command with terminal
There are four projects implemented with "expect" technique on the windows plateform --- activestate expect、chaffee expect、dejagnu and expect.net. Activestate expect and Chaffee expect must install tcl script engine previously and dejagnu runs on the cygwin. Those three ones have to install other tools. The last one "expect.net", it's a program library implemented with .net. It's easy to be familiar with it. So I choose it as my tool to solve the mass program upgrade issue.
The article dicusses about developement of a script engine with expect.net. Operators can write a script for their deployment job easily.

### 3. Requirement for the script engine
The requirement is as following：
1. Use terminal program to control the remote server through ssh
2. Transfer files to the remote server via sftp
3. Mount the USB stick at the remote server
4. Stop the service that are going to upgrade later.
5. Copy the files which is uploaded at step 2 to the specified path, and then start service
6. Notify operators when something goes wrong

### 4. Implementation the engine
#### 4-1. Introduction of the expect .net API
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
"Logining a remote linux server with plink" case as an example to demo expect.net features.
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
Considering the job "Mass upgrade", error management is very important. If something goes wrong during the upgrade process, the tool has to let user know what and where is wrong easily and clearly. Now I design a mechanism that the tool keeps the log for every remote server locally during execution time. The log file name will be replaced with "err" in the tail when something goes wrong during the execution time. The operator just checks the file that filename is with "err" and he can know the problem.

#### 4-4. Arguments of the tool
The script engine designs two execution arguements. Their meaning is as following:
```bs
expect script_file_path remote_server_ip
```
The first one is the path for the execution script
The second one is the remote server ip. The arguments defines the value of the variable '<?ip>' in the script file. 


#### 4-5. The implementation of every script command
The format of the script is as following:
```sh
opcode op1 op2
#opcode is the name of the script command
#op1, op2 is the arguments for the opcode. The number of the argument could be one or two depends on the opcode.
```
The script command is described below:
1.spawn
##### What to do
```sh
#Instantiate a spawnobject object which executes the arguments "execute_command" and "command_args"
spawn spawnobject "execute_command","command_args"
```
The spawnobject represents a commandline windows and operators can use the spawnobject to "expect" or "mount" command. It works like the operator opens a commandline windows and execute some work on it.
##### How to do
The engine uses .net Dictionary class to store the script object. Dictionary uses key and value pair to store information so the two script commands --- spawn and assignpid, their objectname which is defined in the script is stored in the key part of Dictionary otherwise the object instance is store in the value part.
```cs
//the op1 of the script command is stored in the dictionary
spawn_dictionary.Add(op1, ps_results[ps_results.Length - 2]);
//Get the object from dictionary
((Session)spawn_dictionary[param_arr[0]]).Expect("root", (s) => opcode = s);
```

2.expect
##### What to do
There are three types of expect command as followings:
```sh
#1. After executing do_something, wait for the string "expect_string" and check the execution result of the do_something.
spawnobject.expect_with_check expect_string do_something

#2. After executing do_something, wait for the string "expect_string" up to 30 seconds. The command can solve the problem that longer time responsing when file uploaded via ftp.
spawnobject.expect_longtimeout expect_string do_something

#3. A basic command for expect that wait for the string "expect_string", after executing do_something
spawnobject.expect expect_string do_something
```
3.Get process pid and kill pid
##### What to do
```sh
#Get the pid of the process which name is processname，and assign the pid to the processobject
spawnobject.assignpid processobject processname

//kill command must be executed immeduately after assognpid. It can kill the pid stored in processobject
spawnobject.kill processobject
```
##### How to do
```cs
//assign pid
//grep -v "grep" exclude "grep op2" process pid
 ((Session)spawn_dictionary[param_arr[0]]).Send("ps -aux | grep " + op2 + " | grep -v "grep"\n");

 ((Session)spawn_dictionary[param_arr[0]]).Expect("root", (s) => ps_result = s);
 string[] ps_results = ps_result.Split('\n');

 //Get the last record pid
 writelog(fs, "pid:" + ps_results[ps_results.Length - 2]);

 spawn_dictionary.Add(op1, ps_results[ps_results.Length - 2]);

//kill command 
 ((Session)spawn_dictionary[param_arr[0]]).Send("kill " + ((string)spawn_dictionary[op2]) + "\n");
```
4.mount
##### What to do
```bs
#mount usb sticks to the mountpoint path
spawnobject.mountusb usbobject mountpoint
```
##### How to do
Now the implementation just finds the minumum capacity of the disk installed in the server. And the disk is the USB stick.
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
### 5. The usage of the script
We use the tool to rewrite the sction 4.2 from c# code into this script language
```sh
#Instantiate a spawnobject object which name is s1 and execute plink and the argument for the plink is "-l username -pw password <?ip>  -t". And user define <?ip> variable and its value is defined in the second argument of the command line.
spawn s1 "c:\plink.exe","-l username -pw password <?ip>  -t"

#Instantiate a spawnobject object which name is s1 and execute psftp. 
spawn s2 "c:\psftp.exe","-l username -pw password "

#Execute "su" on plink. And send password "1234" after get string "Password:".
s1.expect "Password:" "su"
s1.expect "#" "1234"

#Execute "chmod" and check the execution result on plink. After that expect to get string "#".
s1.expect_with_check "#" "chmod 777 /home/guest"
s1.expect_with_check "#" "mkdir /home/usb"

#mount USB sticks to /home/usb
s1.mountusb osusb /home/usb

#Execute "put" on psftp. And expect to get string "#" with expect_longtimeout command due to longer time spending.
s2.expect_with_check ">" "cd /home/guest/"
s2.expect_longtimeout ">" "put c:\test.txt"

#Get PID when executing execute_pcs on plink and assign the pid to the ncspid object.
s1.assignpid ncspid execute_pcs

#Kill pid storinging in the ncspid object.
s1.kill "#" ncspid

s1.expect_with_check "#" "cp /home/guest/test.txt /home/usb/test/"
s1.expect_with_check "#" "umount /home/usb/"
s1.expect "#" rmdir /home/usb/"
```


### 6. Future work
There are more works can do in the future, such as:
1. Rollback --- If something goes wrong, the engine can recovery the software back to the original state.
2. Find PID --- If there are more than one process created with the same execution file, the engine can list and store all of them. 
3. Mount USB sticks --- Find the USB stick in a more specified way.