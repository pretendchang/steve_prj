using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.UI;
using System.Web.UI.WebControls;

using System.IO;
using System.Text;
using System.Security.Cryptography;
using System.Xml.Linq;

public partial class testSSO : System.Web.UI.Page
{
    public string accesstoken = "";
    public string id = "";
    public string usertoken = "";
    bool error = false;
    protected void Page_Load(object sender, EventArgs e)
    {
        
    }

    protected void btnSend_Click(object sender, EventArgs e)
    {
        try
        {
            SSOLogin.SSOLoginSoapClient client = new SSOLogin.SSOLoginSoapClient();
            XElement xelm = client.GetAccessToken(idtext.Text);
            id = idtext.Text;

            XElement xelmraccess = xelm.Element("accessToken");
            if (xelmraccess == null)
            {
                error = true;
                XElement err = xelm.Element("ErrorCode");
                accesstoken = "無法取得AccessToken，發生原因" + err.Value;
                aspaccesstoken.Value = accesstoken;
            }
            else
            {
                accesstoken = xelmraccess.Value;
                aspaccesstoken.Value = accesstoken;
                aspid.Value = id;
            }
        }
        catch (System.ServiceModel.FaultException ex)
        {
            accesstoken = "無法取得AccessToken，發生原因" + ex.Message;
            aspaccesstoken.Value = accesstoken;
        }
        catch (Exception ex)
        {
            accesstoken = "無法取得AccessToken，發生原因" + ex.Message;
            aspaccesstoken.Value = accesstoken;
        }
    }
    protected void btnButton1_Click(object sender, EventArgs e)
    {
        usertoken = CipherAES.EncryptText(account.Text, accesskey.Text);
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

    public static string EncryptText(string input, string password)
    {
        // Get the bytes of the string
        byte[] bytesToBeEncrypted = UTF8Encoding.UTF8.GetBytes(input);
        byte[] passwordBytes = UTF8Encoding.UTF8.GetBytes(password);

        byte[] bytesEncrypted = AES_Encrypt(bytesToBeEncrypted, passwordBytes);

        string result = Convert.ToBase64String(bytesEncrypted);

        return result;
    }

    public static string EncryptText(string input, byte[] passwordBytes)
    {
        // Get the bytes of the string
        byte[] bytesToBeEncrypted = UTF8Encoding.UTF8.GetBytes(input);

        byte[] bytesEncrypted = AES_Encrypt(bytesToBeEncrypted, passwordBytes);

        string result = Convert.ToBase64String(bytesEncrypted);

        return result;
    }
}