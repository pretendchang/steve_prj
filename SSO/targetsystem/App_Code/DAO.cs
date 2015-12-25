using System;
using System.Collections.Generic;
using System.Web;
using System.Data.SqlClient;
using System.Data;
using System.Threading;
using System.Configuration;


public class DAO
{
		protected SqlConnection myConnection;
        protected SqlCommand mycom;
        SqlTransaction transaction;
        string strSQL = "";
        int _MsgID = 0;
        string _ErrorMsg = "";
        int _RecordsAffected = 0;


        public DAO()
        {
            string conn = ConfigurationManager.ConnectionStrings["ConnectionString"].ConnectionString;
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
                    throw new Exception(openerr);
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

        public int InsertSSO(string id, string accesskey, DateTime createtime, string returnurl, string ip, int tid)
        {
            strSQL = @"INSERT INTO [dbo].[vrsapi_SSO]
                      ([Id],[AccessKey],[CreateTime],[ReturnUrl],[Ip],[tid])
                 VALUES (@id,@accesskey,@createtime,@returnurl,@ip,@tid)";

            myConnection.Open();
            SqlTransaction transaction = myConnection.BeginTransaction();
            mycom = new SqlCommand(strSQL, myConnection);
            mycom.Parameters.Add("@id", SqlDbType.NVarChar, 50).Value = id;
            mycom.Parameters.Add("@accesskey", SqlDbType.NVarChar, 50).Value = accesskey;
            mycom.Parameters.Add("@createtime", SqlDbType.DateTime).Value = createtime;
            mycom.Parameters.Add("@returnurl", SqlDbType.NVarChar, 200).Value = returnurl;
            mycom.Parameters.Add("@ip", SqlDbType.NVarChar, 50).Value = ip;
            mycom.Parameters.Add("@tid", SqlDbType.Int).Value = tid;

            mycom.Transaction = transaction;
            try
            {
                _RecordsAffected += mycom.ExecuteNonQuery();
                _MsgID = 0;
                transaction.Commit();
            }
            catch (SqlException e)
            {
                _ErrorMsg = e.Message;
                _MsgID = 1;
                transaction.Rollback();
                return _MsgID;
            }
            finally
            {
                if (myConnection.State == ConnectionState.Open)
                {
                    myConnection.Close();
                    myConnection.Dispose();
                }
            }
            return _MsgID;
        }

        public int InsertSSOSession(int ssoidx, string accesstokenkey, DateTime createtime, string accesstoken)
        {
            strSQL = @"INSERT INTO [dbo].[vrsapi_ssosession]
                      ([SSOidx],[AccessTokenKey],[AccessToken],[CreateTime])
                 VALUES (@ssoidx,@accesstokenkey,@accesstoken,@createtime)";

            myConnection.Open();
            SqlTransaction transaction = myConnection.BeginTransaction();
            mycom = new SqlCommand(strSQL, myConnection);
            mycom.Parameters.Add("@ssoidx", SqlDbType.Int).Value = ssoidx;
            mycom.Parameters.Add("@accesstokenkey", SqlDbType.NVarChar, 50).Value = accesstokenkey;
            mycom.Parameters.Add("@accesstoken", SqlDbType.NVarChar, 50).Value = accesstoken;
            mycom.Parameters.Add("@createtime", SqlDbType.DateTime).Value = createtime;

            mycom.Transaction = transaction;
            try
            {
                _RecordsAffected += mycom.ExecuteNonQuery();
                _MsgID = 0;
                transaction.Commit();
            }
            catch (SqlException e)
            {
                _ErrorMsg = e.Message;
                _MsgID = 1;
                transaction.Rollback();
                return _MsgID;
            }
            finally
            {
                if (myConnection.State == ConnectionState.Open)
                {
                    myConnection.Close();
                    myConnection.Dispose();
                }
            }
            return _MsgID;
        }

        public int DeleteSSO(int expiredmins)
        {
            strSQL = @"delete [dbo].[vrsapi_sso] where GETDATE()>dateadd(mi,@expiredmins,CreateTime)";

            myConnection.Open();
            SqlTransaction transaction = myConnection.BeginTransaction();
            mycom = new SqlCommand(strSQL, myConnection);
            mycom.Parameters.Add("@expiredmins", SqlDbType.Int).Value = expiredmins;

            mycom.Transaction = transaction;
            try
            {
                _RecordsAffected += mycom.ExecuteNonQuery();
                _MsgID = 0;
                transaction.Commit();
            }
            catch (SqlException e)
            {
                _ErrorMsg = e.Message;
                _MsgID = 1;
                transaction.Rollback();
                return _MsgID;
            }
            finally
            {
                if (myConnection.State == ConnectionState.Open)
                {
                    myConnection.Close();
                    myConnection.Dispose();
                }
            }
            return _MsgID;
        }

        public int DeleteSSOSession(int expiredmins)
        {
            strSQL = @"delete [dbo].[vrsapi_ssosession] where GETDATE()>dateadd(mi,@expiredmins,CreateTime)";

            myConnection.Open();
            SqlTransaction transaction = myConnection.BeginTransaction();
            mycom = new SqlCommand(strSQL, myConnection);
            mycom.Parameters.Add("@expiredmins", SqlDbType.Int).Value = expiredmins;

            mycom.Transaction = transaction;
            try
            {
                _RecordsAffected += mycom.ExecuteNonQuery();
                _MsgID = 0;
                transaction.Commit();
            }
            catch (SqlException e)
            {
                _ErrorMsg = e.Message;
                _MsgID = 1;
                transaction.Rollback();
                return _MsgID;
            }
            finally
            {
                if (myConnection.State == ConnectionState.Open)
                {
                    myConnection.Close();
                    myConnection.Dispose();
                }
            }
            return _MsgID;
        }

        public DataTable VerifySSO(string id)
        {
            SqlDataAdapter dsca = new SqlDataAdapter();

            strSQL = @"SELECT  *
                      FROM [dbo].[vrsapi_sso] 
                  where Id=@id";

            mycom = new SqlCommand(strSQL, myConnection);
            mycom.CommandType = CommandType.Text;
            mycom.Parameters.Add("@id", SqlDbType.NVarChar, 50).Value = id;
            dsca.SelectCommand = mycom;

            DataTable dtUser = new DataTable();
            dsca.Fill(dtUser);
            return dtUser;
        }

        public DataTable VerifySSOSession(int ssoidx, string accesstoken, int expiredmins)
        {
            SqlDataAdapter dsca = new SqlDataAdapter();

            strSQL = @"SELECT  [ssoidx],[accesstoken]
                      FROM [dbo].[vrsapi_ssosession] 
                  where SSOidx=@ssoidx and 
                        AccessToken=@accesstoken and 
                       GETDATE()<=dateadd(mi,@expiredmins,CreateTime)";

            mycom = new SqlCommand(strSQL, myConnection);
            mycom.CommandType = CommandType.Text;
            mycom.Parameters.Add("@ssoidx", SqlDbType.Int).Value = ssoidx;
            mycom.Parameters.Add("@accesstoken", SqlDbType.NVarChar, 50).Value = accesstoken;
            mycom.Parameters.Add("@expiredmins", SqlDbType.Int).Value = expiredmins;
            dsca.SelectCommand = mycom;

            DataTable dtUser = new DataTable();
            dsca.Fill(dtUser);
            return dtUser;
        }
}