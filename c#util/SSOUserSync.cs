using System;
using System.Text;
using System.Collections.Generic;
using System.IO;
using System.Data.SqlClient;
using System.Threading;
using System.Data;
using System.Xml;
using System.Net;
using System.Data.OleDb;

namespace SSOUserSync
{
    class Conf
    {
        public Dictionary<string, string> read()
        {
            Dictionary<string, string> result = new Dictionary<string, string>();
            foreach (string s in File.ReadAllLines("conf"))
            {
                string v = s.Substring(s.IndexOf('=') + 1);
                result.Add(s.Split('=')[0], v);
            }
            return result;
        }
    }

    class SQLConnector
    {
        protected SqlConnection myConnection;
        protected SqlCommand mycom;
        SqlTransaction transaction;

        public static LoggingSSOUserSync logger = LoggingSSOUserSync.Create();

        public SQLConnector(string conn)
        {
            myConnection = new SqlConnection(conn);
        }

        private void OpenConnection()
        {
            int trycount = 0;
            string openerr = "";
            if (myConnection.State == ConnectionState.Open)
                myConnection.Close();

            while (myConnection.State == ConnectionState.Closed)
            {
                if (trycount > 3)
                {
                    throw new SSOUserSyncException(openerr);
                }
                try
                {
                    myConnection.Open();
                }
                catch (SqlException ex)
                {
                    trycount++;
                    openerr = ex.Message;
                    {
                        Thread.Sleep(3000);
                        if (myConnection.State == ConnectionState.Open)
                        {
                            myConnection.Close();
                        }
                    }
                }
            }
        }

        public void SyncUser()
        {
            OpenConnection();
            transaction = myConnection.BeginTransaction();
            try
            {
                InsertData();
                transaction.Commit();
            }
            catch (Exception ex)
            {
                transaction.Rollback();
                throw new SSOUserSyncException("SyncUser  Error");
            }
            finally
            {
                if (myConnection.State == ConnectionState.Open)
                {
                    myConnection.Close();
                }
            }
        }

        private void InsertData()
        {
            int _RecordsAffected = 0;
            string strSQL =
               @"declare @ruid int;
                 declare @cnt int;
                 select @cnt=count(*) from [dbo].[Mn_User] where account=@account;
                 if @cnt=0
                 begin
                   INSERT INTO [dbo].[Mn_User]
                               (tid,areaid, account, name,  password,status, countdown, roleid)
                        values(@tid,@areaid,@account,@name,@password,1,      6000,      @roleid);
                    select @ruid = SCOPE_IDENTITY();
                    insert into Rel_CamGroup_User (cgid,uid) values(@cgid,@ruid);
                 end
                 else if @cnt=1
                 begin
                   select @ruid=uid from [dbo].[Mn_User] where account=@account;
                   update Rel_CamGroup_User set cgid=@cgid where uid=@ruid;
                   update [dbo].[Mn_User] set areaid=@areaid, name=@name, tid=@tid,
                             [password]=@password, roleid=@roleid 
							 where account=@account;
                 end";
            mycom = new SqlCommand(strSQL, myConnection);

            mycom.Transaction = transaction;
            _RecordsAffected = mycom.ExecuteNonQuery();
        }

        public void SearchExist()
        {
            string strSQL = "SELECT * from [dbo].[Mn_User] where account=@account";
            try
            {
                mycom = new SqlCommand(strSQL, myConnection);

                mycom.CommandType = CommandType.Text;
                SqlDataAdapter dsca = new SqlDataAdapter();
                dsca.SelectCommand = mycom;
                DataTable dtEquip = new DataTable();
                dsca.Fill(dtEquip);
                if (dtEquip.Rows.Count >= 1)
                {
                    string s = dtEquip.Rows[0]["account"].ToString();
                }
                else
                {
                    throw new SSOUserSyncException("Error");
                }
            }
            catch (SqlException e)
            {
            }
        }

    }

    interface SSOReader
    {
        bool Read();
        SSOState CheckCondition(out string value);
        void Eatup();
    }

    public class SSOXmlReader : SSOReader
    {
        XmlReader reader;
        public SSOXmlReader(Stream doc)
        {
            if (doc == null)
            {
                throw new SSOUserSyncException("Stream object has to be initialized and opened");
            }
            reader = XmlReader.Create(doc);
        }
        public bool Read()
        {
            return reader.Read();
        }

        public SSOState CheckCondition(out string value)
        {
            if (reader.Name == "UserDataSN" && reader.NodeType != XmlNodeType.EndElement)
            {//state1
                value = null;
                return SSOState.UserDataSN_Start;
            }
            else if (reader.Name == "getMemberID" && reader.NodeType != XmlNodeType.EndElement)
            {//state2
                value = reader.ReadElementString();
                return SSOState.getMemberID;
            }
            else if (reader.Name == "getMemberBranchVrsNo" && reader.NodeType != XmlNodeType.EndElement)
            {//state3
                value = reader.ReadElementString();
                return SSOState.getMemberBranchVrsNo;
            }
            else if (reader.Name == "getMemberName" && reader.NodeType != XmlNodeType.EndElement)
            {//state4
                value = reader.ReadElementString();
                return SSOState.getMemberName;
            }
            else if (reader.Name == "getMemberStationVrsNo" && reader.NodeType != XmlNodeType.EndElement)
            {//state5
                value = reader.ReadElementString();
                return SSOState.getMemberStationVrsNo;
            }
            else if (reader.Name == "getMemberTel" && reader.NodeType != XmlNodeType.EndElement)
            {//state6
                value = reader.ReadElementString();
                return SSOState.getMemberTel;
            }
            else if (reader.Name == "getMemberEmail" && reader.NodeType != XmlNodeType.EndElement)
            {//state7
                value = reader.ReadElementString();
                return SSOState.getMemberEmail;
            }
            if (reader.Name == "UserDataSN" && reader.NodeType == XmlNodeType.EndElement)
            {//state8
                value = null;
                return SSOState.UserDataSN_End;
            }
            else if (reader.Name == "getMemberStation" && reader.NodeType != XmlNodeType.EndElement)
            {//state10
                value = reader.ReadElementString();
                return SSOState.getMemberStation;
            }
            else if (reader.Name == "getMemberBranch" && reader.NodeType != XmlNodeType.EndElement)
            {//state11
                value = reader.ReadElementString();
                return SSOState.getMemberBranch;
            }
            value = null;
            return SSOState.NoState;
        }

        public void Eatup()
        {
            while (reader.Name != "UserDataSN" || reader.NodeType != XmlNodeType.EndElement)
            {//eat up the rest of the xml
                reader.Read();
            }
        }
    }

    public class SSOExcelReader : SSOReader
    {
        int rowcount = 0;
        int colcount = -1;
        DataTable objAccount;
        public SSOExcelReader()
        {
            string path = @"C:\Users\stevechang\Desktop\b.xlsx";
            string strCon = " Provider = Microsoft.ACE.OLEDB.12.0 ; Data Source = " + path + ";Extended Properties='Excel 12.0;HDR=N'";
            OleDbConnection objConn = new OleDbConnection(strCon);

            string strCom = " SELECT * FROM [a$] ";
            objConn.Open();

            OleDbDataAdapter objCmd = new OleDbDataAdapter(strCom, objConn);
            objAccount = new DataTable();
            objCmd.Fill(objAccount);
            objConn.Close();
        }
        public bool Read()
        {
            colcount++;
            if (colcount < 10)
                return true;
            else
            {
                colcount = 0;
                rowcount++;
                if (rowcount < objAccount.Rows.Count)
                    return true;
                else
                    return false;
            }
        }

        public SSOState CheckCondition(out string value)
        {
            try
            {
                switch (colcount)
                {
                    case 0:
                        value = null;
                        return SSOState.UserDataSN_Start;
                    case 1:
                        value = objAccount.Rows[rowcount][0].ToString();
                        return SSOState.getMemberID;
                    case 2:
                        value = objAccount.Rows[rowcount][1].ToString();
                        return SSOState.getMemberBranchVrsNo;
                    case 3:
                        value = objAccount.Rows[rowcount][2].ToString();
                        return SSOState.getMemberName;
                    case 4:
                        value = objAccount.Rows[rowcount][3].ToString();
                        return SSOState.getMemberStationVrsNo;
                    case 5:
                        value = objAccount.Rows[rowcount][4].ToString();
                        return SSOState.getMemberTel;
                    case 6:
                        value = objAccount.Rows[rowcount][5].ToString();
                        return SSOState.getMemberEmail;
                    case 7:
                        value = objAccount.Rows[rowcount][6].ToString();
                        return SSOState.getMemberStation;
                    case 8:
                        value = objAccount.Rows[rowcount][7].ToString();
                        return SSOState.getMemberBranch;
                    case 9:
                        value = null;
                        return SSOState.UserDataSN_End;
                }
                value = null;
                return SSOState.NoState;
            }
            catch (Exception ex)
            {
                value = ex.Message;
                return SSOState.OnError;
            }
        }

        public void Eatup()
        {
            colcount = objAccount.Columns.Count;
        }
    }
    public class BO
    {
    }

    class XMLProtocol
    {
        /*
<ArrayOfUserDataSN>
<UserDataSN>
  <getMemberID>b121927660</getMemberID> 
  <getMemberName>陳昱帆</getMemberName> 
  <getMemberBranch>平鎮分局</getMemberBranch> 
  <getMemberStation>平鎮所</getMemberStation> 
  <getMemberTel>03-4938381</getMemberTel> 
  <getMemberEmail /> 
  <getMemberBranchVrsNo>286</getMemberBranchVrsNo> //areaid
  <getMemberStationVrsNo>4072</getMemberStationVrsNo> //cgid
</UserDataSN>
</<ArrayOfUserDataSN>
         */

        private const int MAX_ACCOUNT_STRING_LENGTH = 50;
        public static LoggingSSOUserSync logger = LoggingSSOUserSync.Create();

        public XMLProtocol(Action _action)
        {
            if (_action == null)
            {
                throw new SSOUserSyncException("Action object has to be initialized");
            }
            action = _action;
        }

        private Action action;
        SSOReader reader;
        public Tx parse(SSOReader _reader)
        {
            if (_reader == null)
            {
                throw new SSOUserSyncException("SSOReader has to be initialized");
            }
            reader = _reader;
            Tx tx = action.DoInitTx(DateTime.Now.Ticks.ToString());
            string title = "";
            while (reader.Read())
            {
                try
                {
                    string value;
                    SSOState state = reader.CheckCondition(out value);
                    action.DoAction((int)state, value);
                    if (state == SSOState.getMemberID)
                    {
                        action.DoSetTitle(value); title = value;
                    }
                }
                catch (Exception ex)
                {
                    action.DoAction((int)SSOState.OnError, ex.Message);
                    logger.Error(title + " handling error: " + ex.Message);
                    reader.Eatup();
                }
            }
            return tx;
        }

        public Dictionary<string, BO> parse(XmlDocument doc)
        {
            XmlNodeList usernodes = doc.SelectNodes("/UserDataSN");
            XmlNodeList accountnodes = doc.SelectNodes("/UserDataSN/getMemberID");
            XmlNodeList areaidnodes = doc.SelectNodes("/UserDataSN/getMemberBranchVrsNo");
            Dictionary<string, BO> bos = new Dictionary<string, BO>();
            for (int i = 0; i < usernodes.Count; i++)
            {
                string account = accountnodes.Item(i).InnerText;
                if (account.Length > MAX_ACCOUNT_STRING_LENGTH)
                {
                    logger.Warn(account + ":exceeds the string max limit 50");
                    continue;
                }

                int areaid = Convert.ToInt32(areaidnodes.Item(i).InnerText);
                //bos.Add(account, new BO(account, areaid));
            }
            return bos;

        }
    }

    class WebServiceConnector<TWebServiceResult>
    {
        public const string connstr = "";
        private Type service;
        HttpWebRequest req;

        public WebResponse GetDataStreamToSyncViaHttpGet(string url)
        {
            req = (HttpWebRequest)HttpWebRequest.Create(url);
            req.Method = "POST";
            req.ContentType = "application/x-www-form-urlencoded";
            req.ContentLength = 0; return req.GetResponse();
        }

        
    }

    public class Tx
    {
        public int txidx;
        public string txno = "";
        public DateTime begintime;
        public DateTime endtime;
        public int status = 1;
    }

    public class TxDetail
    {
        public int txidx;
        public string account = "";
        public string txno = "";
        public int status = 1;
        public string note = "";
    }

    public class LoggingSSOUserSync
    {
        //requirement
        //1. logging in the db
        //     TxStatus:  idx, txno, begintime, endtime, status
        //     TxStatusDetail: idx, Txstatusidx, account, status, note

        //2. response status of the user sync back to the remotor
        //3. notification the result

        private static LoggingSSOUserSync logger;
        private string msg;

        private LoggingSSOUserSync()
        {
            msg = "";
        }

        public static LoggingSSOUserSync Create()
        {
            if (logger == null)
                logger = new LoggingSSOUserSync();
            return logger;
        }

        public string GetMsg()
        {
            return msg;
        }

        public void Debug(string s)
        {
        }

        public void Warn(string s)
        {
            msg += s + "\r\n";
        }

        public void Error(string s)
        {
            msg += s + "\r\n";
            //write db
        }

        public void Fatal(string s)
        {
            msg += s + "\r\n";
            //writedb
        }
    }

    public class SSOUserSyncException : Exception
    {
        public string msg;

        public SSOUserSyncException(string s)
            : base(s)
        {
            msg = s;
        }
    }

    public delegate int LogTx(Tx tx);
    public delegate void LogTxDetail(TxDetail txdetail);
    public delegate int InsertArea(string areaname);
    public delegate int InsertCamGroup(string name, int areaid);

    public enum SSOState : int
    {
        UserDataSN_Start = 1,
        getMemberID = 2,
        getMemberBranchVrsNo = 3,
        getMemberName = 4,
        getMemberStationVrsNo = 5,
        getMemberTel = 6,
        getMemberEmail = 7,
        UserDataSN_End = 8,
        OnError = 9,
        getMemberStation = 10,
        getMemberBranch = 11,
        NoState = 100
    }

    interface Action
    {
        void DoAction(int state, string value);
        Tx DoInitTx(string txno);
        void DoSetTitle(string title);
    }

    public class DelegateGroup
    {
        public LogTx _logtx;
        public LogTxDetail _logtxdetail;
        public InsertArea _insertarea;
        public InsertCamGroup _insertcamgroup;
    }

    public class Action3 : Action
    {//testconnect
        DelegateGroup dg;
        Tx tx;
        public static LoggingSSOUserSync logger = LoggingSSOUserSync.Create();
        public Action3()
        {
            dg = new DelegateGroup();
        }

        public Tx DoInitTx(string txno)
        {
            tx = new Tx();
            return tx;

        }
        public void DoSetTitle(string title)
        {
        }
        public void DoAction(int state, string value)
        {
            switch (state)
            {
                case (int)SSOState.UserDataSN_Start:
                    logger.Debug("UserDataSN:start");
                    break;
                case (int)SSOState.getMemberID:

                    break;
                case (int)SSOState.getMemberBranchVrsNo:
                    logger.Debug("getMemberBranchVrsNo:" + value);
                    if (value == null)
                    {
                        value = "";
                    }
                    value = value.Trim();
                    if (value == "")
                    {
                        throw new SSOUserSyncException("Account:" + value + " getMemberBranchVrsNo is empty");
                    }

                    break;
                case (int)SSOState.getMemberName:
                    logger.Debug("getMemberBranchVrsNo:" + value);

                    break;
                case (int)SSOState.getMemberStationVrsNo:
                    logger.Debug("getMemberBranchVrsNo:" + value);
                    if (value == null)
                    {
                        value = "";
                    }
                    value = value.Trim();
                    if (value == "")
                    {
                        throw new SSOUserSyncException("Account:" + value + " getMemberStationVrsNo is empty");
                    }

                    break;
                case (int)SSOState.UserDataSN_End:
                    logger.Debug("UserDataSN:end");
                    break;
                case (int)SSOState.OnError:
                    break;
            }
        }
    }

    public class testSSOUserSync
    {
        public static LoggingSSOUserSync logger = LoggingSSOUserSync.Create();
        public static void testConn()
        {
            WebResponse resp = null;
            try
            {
                Conf c = new Conf();
                Dictionary<string, string> conf = c.read();
                string url = "";
                conf.TryGetValue("url", out url);

                url = "http://localhost:53013/WebSite2/WebService.asmx/HelloWorld";

                //Get input stream, it could be a file
                WebServiceConnector<XmlElement> wc = new WebServiceConnector<XmlElement>();
                resp = wc.GetDataStreamToSyncViaHttpGet(url);

                string conn = "";
                conf.TryGetValue("conn", out conn);
                SQLConnector sql = new SQLConnector(conn);

                Action action = new Action3();
                XMLProtocol xml = new XMLProtocol(action);

                Stream stream = resp.GetResponseStream();
                SSOXmlReader reader = new SSOXmlReader(resp.GetResponseStream());
                xml.parse(reader);
            }
            catch (Exception ex)
            {
                logger.Fatal(ex.Message);
            }
            finally
            {
                if (resp != null)
                {
                    resp.GetResponseStream().Close();
                    resp.Close();
                }
            }
        }
        public static void testConnExcel()
        {
            try
            {
                Conf c = new Conf();
                Dictionary<string, string> conf = c.read();

                string conn = "";
                conf.TryGetValue("conn", out conn);
                SQLConnector sql = new SQLConnector(conn);

                Action action = new Action3();
                XMLProtocol xml = new XMLProtocol(action);

                SSOExcelReader reader = new SSOExcelReader();
                xml.parse(reader);
            }
            catch (Exception ex)
            {
                logger.Fatal(ex.Message);
            }

        }
    }
}
