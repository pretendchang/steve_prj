using System;
using System.Collections.Generic;
using System.Web;
using System.Web.Services;
using System.Web.Services.Protocols;
using System.Xml;
using System.Data;
using System.Threading;
using System.Security.Cryptography;
using System.Text;
using System.Web.Script.Services;


[WebService(Namespace = "http://tempuri.org/")]
[WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]

public class SSOLogin : System.Web.Services.WebService {

    private const int _accessTokenExpiredMins = 20;

    public SSOLogin () {

        sso = new SSOAccount();
    }

    [WebMethod]
    [ScriptMethod(ResponseFormat = ResponseFormat.Xml)]
    public XmlDocument GetAccessToken(string id)
    {
        XmlDocument xmlDoc = new XmlDocument();
        XmlElement result = xmlDoc.CreateElement("Result");
        XmlElement XmlElmAccessToken = xmlDoc.CreateElement("accessToken");
        XmlElement XmlElmErrorCode = xmlDoc.CreateElement("ErrorCode");

        try
        {
            VerifyID(id);
            string accesstoken = GenerateAccessToken(id);
            XmlElmAccessToken.InnerText = accesstoken;
            result.AppendChild(XmlElmAccessToken);
            xmlDoc.AppendChild(result);
        }
        catch (SSOLogingException ex)
        {
            throw new SoapException(ex.msg, SoapException.ServerFaultCode);
        }
        catch (Exception ex)
        {
            throw new SoapException(ex.Message, SoapException.ServerFaultCode);
        }

        return xmlDoc;
    }

    internal class SSOAccount
    {
        public int idx = 0;
        public string id = "";
        public string ip = "";

    }
    private SSOAccount sso;

    private void VerifyID(string id)
    {
        DAO dao = new DAO();
        DataTable dt = dao.VerifySSO(id);
        if (dt.Rows.Count == 1)
        {
            sso.idx = Convert.ToInt32(dt.Rows[0]["idx"]);
            sso.id = dt.Rows[0]["id"].ToString();
            sso.ip = dt.Rows[0]["ip"].ToString();
        }
        else
            throw new SSOLogingException("Id is error");
    }

    private string GenerateAccessToken(string id)
    {
        //threadid+time encrypt with sha256 for accesstoken
        string accesstoeknkey = Thread.CurrentThread.ManagedThreadId.ToString() + DateTime.Now.Ticks.ToString();
        string AccessToken = SHA256Encrypt.EncryptText(accesstoeknkey).Replace("-", string.Empty);

        //sso session write into db
        DAO dao = new DAO();
        dao.InsertSSOSession(sso.idx, accesstoeknkey, DateTime.Now, AccessToken);
        //delete expired ssosession
        dao = new DAO();
        dao.DeleteSSOSession(_accessTokenExpiredMins);

        return AccessToken;
    }
    
}

public class SSOLogingException : Exception
{
    public string msg;
    public SSOLogingException(string s)
    {
        msg = s;
    }
    public SSOLogingException()
    {
        msg = "";
    }
}

public class SHA256Encrypt
{
    public static string EncryptText(string input)
    {
        // Get the bytes of the string
        SHA256 sha256 = new SHA256Managed();
        byte[] b = sha256.ComputeHash(Encoding.UTF8.GetBytes(input));

        string result = BitConverter.ToString(b);

        return result;
    }
}
