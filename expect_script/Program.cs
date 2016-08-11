using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ExpectNet;
using System.Threading;
using System.IO;
using System.Collections;
//using System.Text.RegularExpressions;

namespace expecttest1
{
    class Program
    {
        static string logfilename="";
        static FileStream fs;
        static int error = 0;
        static int check_error(string opcode)
        {
            char[] opsplitter = { '\n' };
            if (opcode.Split(opsplitter).Length > 2)
            {
                writelog(fs, "error:" + opcode);
                error = 1;
                return 1;
            }
            else
            {
                writelog(fs, "ok:" + opcode);
            }
            return 0;
        }
        static void writelog(FileStream _fs, string s)
        {
            StreamWriter sw = new StreamWriter(_fs);
            sw.WriteLine(s);
            sw.Flush();
            Console.WriteLine(s);
        }
        static void renamelog()
        {
            System.IO.File.Move(logfilename, logfilename+"_err");
        }
        static FileStream openlog(string ip)
        {
            logfilename = "auto_" + ip + "_" + DateTime.Now.Ticks;

            return File.OpenWrite(logfilename);
        }
        static void Main(string[] args)
        {
            //第一個參數是欲執行script檔路徑
            //第二個參數是script檔中的<?ip>變數的值
            try
            {
                //系統中spawn和assignpid兩個指令，會定義物件，script中的第二個參數物件名字就放在dictionary的key中，對應expect.net物件，儲存在value
                Dictionary<string, Object> spawn_dictionary = new Dictionary<string, Object>();
                fs = openlog(args[1]);
                foreach (string line in File.ReadLines(args[0]))
                {
                    //建立一個spawnobject物件，執行execute_command command_args
                    //spawn spawnobject "execute_command","command_args"
                    if (line.StartsWith("spawn") == true)
                    {
                        int op1_index, op2_index;
                        string opcode, op1, op2;
                        string[] param_arr;
                        char[] splitter = { ',' };
                        op1_index = line.IndexOf(' ');
                        opcode = line.Substring(0, op1_index);
                        op2_index = line.IndexOf(' ', op1_index + 1);
                        op1 = line.Substring(op1_index + 1, op2_index - op1_index - 1);
                        op2 = line.Substring(op2_index + 1);
                        param_arr = op2.Split(splitter, 2);
                        param_arr[0] = param_arr[0].Replace("\"", ""); param_arr[1] = param_arr[1].Replace("\"", "");
                        param_arr[1] = param_arr[1].Replace("<?ip>", args[1]);
                        Session spawn = Expect.Spawn(new ProcessSpawnable(param_arr[0], param_arr[1]));
                        spawn_dictionary.Add(op1, spawn);
                        if (param_arr[0].Contains("plink"))
                            ((Session)spawn_dictionary[op1]).Expect("$", (s) => writelog(fs, "ok: " + s));
                        else if (param_arr[0].Contains("psftp"))
                            ((Session)spawn_dictionary[op1]).Expect(">", (s) => writelog(fs, "ok: " + s));
                        //Console.WriteLine(opcode+":"+op1+":"+op2+":"+param_arr[0]+":"+param_arr[1]);
                    }
                    ////執行do_something後，等待終端機回傳內含expect_string字串，並且檢查do_something執行結果，若在timeout時間內等到，則印出字串
                    else if (line.Contains(".expect_with_check") == true)
                    {
                        int op1_index, op2_index;
                        string opcode, op1, op2;
                        string[] param_arr;
                        char[] splitter = { '.' };

                        op1_index = line.IndexOf(' ');
                        opcode = line.Substring(0, op1_index);
                        op2_index = line.IndexOf(' ', op1_index + 1);
                        op1 = line.Substring(op1_index + 1, op2_index - op1_index - 1).Replace("\"", "");
                        op2 = line.Substring(op2_index + 1).Replace("\"", "");
                        op2 = op2.PadRight(op2.Length + 1, '\n');

                        param_arr = opcode.Split(splitter, 2);
                        // Console.WriteLine(opcode + ":" + op1 + ":" + op2 + ":" + param_arr[0] + ":" + param_arr[1]);

                        ((Session)spawn_dictionary[param_arr[0]]).Send(op2);
                        //((Session)spawn_dictionary[param_arr[0]]).Expect(op1, (s) => Console.WriteLine("found: " + s));

                        ((Session)spawn_dictionary[param_arr[0]]).Expect(op1, (s) => opcode = s);
                        check_error(opcode);
                    }
                    //執行do_something後，等待終端機回傳內含expect_string字串，等待時間拉長到30s，若在timeout時間內等到，則印出字串，此指令解決ftp檔案上傳，指令回應速度較慢的問題
                    else if (line.Contains(".expect_longtimeout") == true)
                    {
                        int op1_index, op2_index;
                        string opcode, op1, op2;
                        string[] param_arr;
                        char[] splitter = { '.' };

                        op1_index = line.IndexOf(' ');
                        opcode = line.Substring(0, op1_index);
                        op2_index = line.IndexOf(' ', op1_index + 1);
                        op1 = line.Substring(op1_index + 1, op2_index - op1_index - 1).Replace("\"", "");
                        op2 = line.Substring(op2_index + 1).Replace("\"", "");
                        op2 = op2.PadRight(op2.Length + 1, '\n');

                        param_arr = opcode.Split(splitter, 2);
                        //Console.WriteLine(opcode + ":" + op1 + ":" + op2 + ":" + param_arr[0] + ":" + param_arr[1]);
                        ((Session)spawn_dictionary[param_arr[0]]).Timeout = 30000;//set to 30s
                        ((Session)spawn_dictionary[param_arr[0]]).Send(op2);
                        ((Session)spawn_dictionary[param_arr[0]]).Expect(op1, (s) => writelog(fs, "ok: " + s));
                        ((Session)spawn_dictionary[param_arr[0]]).Timeout = 2500;//set back to default value 2.5s
                    }
                    //執行do_something後，等待終端機回傳內含expect_string字串，若在timeout時間內等到，則印出字串
                    else if (line.Contains(".expect") == true)
                    {
                        int op1_index, op2_index;
                        string opcode, op1, op2;
                        string[] param_arr;
                        char[] splitter = { '.' };

                        op1_index = line.IndexOf(' ');
                        opcode = line.Substring(0, op1_index);
                        op2_index = line.IndexOf(' ', op1_index + 1);
                        op1 = line.Substring(op1_index + 1, op2_index - op1_index - 1).Replace("\"", "");
                        op2 = line.Substring(op2_index + 1).Replace("\"", "");
                        op2 = op2.PadRight(op2.Length + 1, '\n');

                        param_arr = opcode.Split(splitter, 2);
                        //Console.WriteLine(opcode + ":" + op1 + ":" + op2 + ":" + param_arr[0] + ":" + param_arr[1]);

                        ((Session)spawn_dictionary[param_arr[0]]).Send(op2);
                        ((Session)spawn_dictionary[param_arr[0]]).Expect(op1, (s) => writelog(fs, "ok: " + s));
                    }
                    //取得processname執行的pid，並將值指定到processobject中
                    else if (line.Contains(".assignpid") == true)
                    {
                        int op1_index, op2_index;
                        string opcode, op1, op2, ps_result = "";
                        string[] param_arr;
                        char[] splitter = { '.' };

                        op1_index = line.IndexOf(' ');
                        opcode = line.Substring(0, op1_index);
                        op2_index = line.IndexOf(' ', op1_index + 1);
                        op1 = line.Substring(op1_index + 1, op2_index - op1_index - 1);
                        op2 = line.Substring(op2_index + 1);
                        param_arr = opcode.Split(splitter, 2);
                        //grep -v "grep" 排除掉grep op2的pid
                        ((Session)spawn_dictionary[param_arr[0]]).Send("ps -aux | grep " + op2 + " | awk '{print $2}' | tail -n2 | head -n1\n");

                        ((Session)spawn_dictionary[param_arr[0]]).Expect("root", (s) => ps_result = s);
                        string[] ps_results = ps_result.Split('\n');
                        writelog(fs, "pid:" + ps_results[ps_results.Length - 2]);
                        //取得最後一筆pid(若該process有多個pid，只能取一個)
                        spawn_dictionary.Add(op1, ps_results[ps_results.Length - 2]);
                    }
                    //kill指令需緊跟著assignpid後，殺掉processobject中的pid
                    else if (line.Contains(".kill") == true)
                    {
                        int op1_index, op2_index;
                        string opcode, op1, op2;
                        string[] param_arr;
                        char[] splitter = { '.' };

                        op1_index = line.IndexOf(' ');
                        opcode = line.Substring(0, op1_index);
                        op2_index = line.IndexOf(' ', op1_index + 1);
                        op1 = line.Substring(op1_index + 1, op2_index - op1_index - 1).Replace("\"", "");
                        op2 = line.Substring(op2_index + 1);
                        param_arr = opcode.Split(splitter, 2);

                        //((Session)spawn_dictionary[param_arr[0]]).Expect("root", (s) => Console.WriteLine("found1: " + s));
                        ((Session)spawn_dictionary[param_arr[0]]).Send("kill " + ((string)spawn_dictionary[op2]) + "\n");
                        //((Session)spawn_dictionary[param_arr[0]]).Expect("root", (s) => Console.WriteLine("found: " + s));
                        ((Session)spawn_dictionary[param_arr[0]]).Expect("root", (s) => opcode = s);
                        check_error(opcode);
                    }
                    //mount隨身碟到mountpoint路徑
                    else if (line.Contains(".mountusb") == true)
                    {
                        int op1_index, op2_index;
                        string opcode, op1, op2;
                        string[] param_arr;
                        char[] splitter = { '.' };

                        op1_index = line.IndexOf(' ');
                        opcode = line.Substring(0, op1_index);
                        op2_index = line.IndexOf(' ', op1_index + 1);
                        op1 = line.Substring(op1_index + 1, op2_index - op1_index - 1).Replace("\"", "");
                        op2 = line.Substring(op2_index + 1);
                        param_arr = opcode.Split(splitter, 2);
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
                        ((Session)spawn_dictionary[param_arr[0]]).Expect("root", (s) => opcode = s);
                        check_error(opcode);
                    }

                }
            }
            catch (Exception e)
            {
                writelog(fs, e.ToString());
                //Console.Error.WriteLine(e);
                error = 1;
            }
            finally
            {
                fs.Close();
                if (error == 1)
                    renamelog();
            }
           // Console.ReadKey();
        }

        static void testCommand(string ts)
        {
            Session spawn = Expect.Spawn(new ProcessSpawnable("c:\\plink.exe", "-l user -pw password 192.168.13.77 -t"));
            Session spawn2 = Expect.Spawn(new ProcessSpawnable("c:\\psftp.exe", "-l user -pw password 192.168.13.77"));
            spawn.Expect("$", s => Console.WriteLine("got: " + s));
            spawn.Send("su\n");
            spawn.Expect("Password:", (s) => Console.WriteLine("found: " + s));
            spawn.Send("su_password\n");
            spawn.Expect("root", (s) => Console.WriteLine("found: " + s));
            spawn.Send(ts);

            spawn.Expect("root", (s) => Console.WriteLine("found1: " + s));
        }

        static void testscript()
        {
            testCommand("ps -aux | grep ncs_core | grep -v \"grep\" | awk '{print $2}' | tail -n2 | head -n1 ");
        }
    }
}
