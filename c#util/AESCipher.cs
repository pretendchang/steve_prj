using System;
using System.Security.Cryptography;
using System.IO;
using System.Text;
namespace ConsoleApplication2
{
    public class testExample
    {
        public static void testCipherAES()
        {
            string s = CipherAES.EncryptText("beta1962", "12345678901234567890123456789012");
            s = CipherAES.DecryptText(s, "12345678901234567890123456789012");
        }

        private byte GetDec(char c)
        {
            switch (c)
            {
                case '0':
                    return 0;
                case '1':
                    return 1;
                case '2':
                    return 2;
                case '3':
                    return 3;
                case '4':
                    return 4;
                case '5':
                    return 5;
                case '6':
                    return 6;
                case '7':
                    return 7;
                case '8':
                    return 8;
                case '9':
                    return 9;
                case 'a':
                    return 10;
                case 'b':
                    return 11;
                case 'c':
                    return 12;
                case 'd':
                    return 13;
                case 'e':
                    return 14;
                case 'f':
                    return 15;

                default:
                    return 0;
            }
        }

        private byte[] GetKey(string _accessKey)
        {
            byte[] k = new byte[32];
            char[] c = _accessKey.ToCharArray();
            int j = 0;
            for (int i = 0; i < c.Length; i += 2)
            {

                k[j] = GetDec(c[i]);
                k[j] = (byte)(k[j] * 16);

                k[j] += GetDec(c[i + 1]);
                j++;
            }


            return k;
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
}
