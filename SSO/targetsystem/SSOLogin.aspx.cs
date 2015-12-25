using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.UI;
using System.Web.UI.WebControls;

using System.Net;
using System.IO;
using System.Text;
using System.Security.Cryptography;
using System.Data;

public partial class SSOLogin : System.Web.UI.Page
{
    private string _id = "";
    private string _accessToken = "";
    private string _accessKey = "";
    private string _userToken = "";
    private string _strUID = "";
    private const int _accessTokenExpiredMins = 20;

    protected void Page_Load(object sender, EventArgs e)
    {
        try
        {
            ValidateLogin(Request.Form["id"], Request.Form["accessToken"], Request.Form["userToken"]);
            DecodeUserToken();
            LdapUserLogin(_strUID);
        }
        catch (SSOLogingaspxException ex)
        {
            ShowMsg(ex.msg);
        }
        catch (FormatException ex)
        {
            ShowMsg(ex.Message);
        }
        catch (CryptographicException ex)
        {
            ShowMsg("userTokenError:" + ex.Message);
        }
        catch (Exception ex)
        {
            ShowMsg(ex.Message);
        }
    }

    internal class SSOAccount
    {
        public int idx = 0;
        public string id = "";
        public string ip = "";
        public string ReturnUrl = "";
    }
    private SSOAccount sso = new SSOAccount();

    private void ValidateLogin(object id, object accessToken, object userToken)
    {
        if (id == null || id.ToString() == "")
            throw new SSOLogingaspxException("Id is error");
        else
            _id = id.ToString().Trim();

        if (accessToken == null || accessToken.ToString() == "")
            throw new SSOLogingaspxException("accessToken is error");
        else
            _accessToken = accessToken.ToString().Trim();

        if (userToken == null || userToken.ToString() == "")
            throw new SSOLogingaspxException("userToken is error");
        else
            _userToken = userToken.ToString().Trim();

        DAO dao = new DAO();
        DataTable dtsso = dao.VerifySSO(_id);
        if (dtsso.Rows.Count == 1)
        {
            sso.idx = Convert.ToInt32(dtsso.Rows[0]["idx"]);
            sso.id = dtsso.Rows[0]["id"].ToString();
            sso.ip = dtsso.Rows[0]["ip"].ToString();
            sso.ReturnUrl = dtsso.Rows[0]["ReturnUrl"].ToString();
            string s = dtsso.Rows[0]["AccessKey"].ToString();
            if (s.Length > 32)
            {
                s = s.Substring(0, 32);
            }
            _accessKey = s;
        }
        else
            throw new SSOLogingaspxException("ID can't be found");

        DataTable dt = dao.VerifySSOSession(sso.idx, _accessToken, _accessTokenExpiredMins);
        if (dt.Rows.Count != 1)
        {
            throw new SSOLogingaspxException("AccessToken matches error");
        }
    }

    private void DecodeUserToken()
    {
        _strUID = CipherAES.DecryptText(_userToken, _accessKey);
    }

    protected void LdapUserLogin(string userName)
    {
        Page.ClientScript.RegisterStartupScript(this.GetType(), "bb", "alert('" + userName + " login done');", true);
    }

    protected void ShowMsg(string strMsg)
    {
        string sbScript;

        sbScript = "alert('" + strMsg + "');" + "\n" + "location.href='" + sso.ReturnUrl + "';";

        Page.ClientScript.RegisterClientScriptBlock(this.GetType(), "bb", sbScript, true);
    }
}

public class SSOLogingaspxException : Exception
{
    public string msg;
    public SSOLogingaspxException(string s)
    {
        msg = s;
    }
    public SSOLogingaspxException()
    {
        msg = "";
    }
}

public class CipherAES
{
    private const int AESKeySize_256 = 256;
    private const PaddingMode AESPadding_PKCS7 = PaddingMode.PKCS7;
    private const CipherMode AESCipherMode_ECB = CipherMode.ECB;

    private static byte[] AES_Encrypt(byte[] bytesToBeEncrypted, byte[] passwordBytes)
    {
        byte[] encryptedBytes = null;

        using (MemoryStream ms = new MemoryStream())
        {
            using (RijndaelManaged AES = new RijndaelManaged())
            {
                AES.KeySize = AESKeySize_256;
                AES.Key = passwordBytes;
                AES.Mode = AESCipherMode_ECB;
                AES.Padding = AESPadding_PKCS7;

                using (CryptoStream cs = new CryptoStream(ms, AES.CreateEncryptor(), CryptoStreamMode.Write))
                {
                    cs.Write(bytesToBeEncrypted, 0, bytesToBeEncrypted.Length);
                    cs.Close();
                }
                encryptedBytes = ms.ToArray();
            }
        }

        return encryptedBytes;
    }

    private static byte[] AES_Decrypt(byte[] bytesToBeDecrypted, byte[] passwordBytes)
    {
        byte[] decryptedBytes = null;

        using (MemoryStream ms = new MemoryStream())
        {
            using (RijndaelManaged AES = new RijndaelManaged())
            {
                try
                {
                    AES.KeySize = AESKeySize_256;
                    AES.Key = passwordBytes;
                    AES.Mode = AESCipherMode_ECB;
                    AES.Padding = AESPadding_PKCS7;

                    using (CryptoStream cs = new CryptoStream(ms, AES.CreateDecryptor(), CryptoStreamMode.Write))
                    {
                        cs.Write(bytesToBeDecrypted, 0, bytesToBeDecrypted.Length);
                        cs.Close();
                    }
                    decryptedBytes = ms.ToArray();
                }
                catch (CryptographicException e)
                {
                    throw e;
                }
            }
        }

        return decryptedBytes;
    }

    public static string DecryptText(string input, string password)
    {
        // Get the bytes of the string
        byte[] bytesToBeDecrypted = Convert.FromBase64String(input);
        byte[] passwordBytes = UTF8Encoding.UTF8.GetBytes(password);

        byte[] bytesDecrypted = AES_Decrypt(bytesToBeDecrypted, passwordBytes);

        string result = "";
        if (bytesDecrypted != null)
            result = UTF8Encoding.UTF8.GetString(bytesDecrypted);

        return result;
    }

    public static string DecryptText(string input, byte[] passwordBytes)
    {
        // Get the bytes of the string
        byte[] bytesToBeDecrypted = Convert.FromBase64String(input);


        byte[] bytesDecrypted = AES_Decrypt(bytesToBeDecrypted, passwordBytes);

        string result = "";
        if (bytesDecrypted != null)
            result = UTF8Encoding.UTF8.GetString(bytesDecrypted);

        return result;
    }

    public static string EncryptText(string input, string password)
    {
        // Get the bytes of the string
        byte[] bytesToBeEncrypted = UTF8Encoding.UTF8.GetBytes(input);
        byte[] passwordBytes = UTF8Encoding.UTF8.GetBytes(password);

        byte[] bytesEncrypted = AES_Encrypt(bytesToBeEncrypted, passwordBytes);

        string result = Convert.ToBase64String(bytesEncrypted);

        return result;
    }
}