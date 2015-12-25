<%@ Page Language="C#" AutoEventWireup="true" CodeFile="testSSO.aspx.cs" Inherits="testSSO" %>

<!DOCTYPE html>

<html xmlns="http://www.w3.org/1999/xhtml">
<head id="Head1" runat="server">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title></title>
</head>
<body>


    <form runat="server" id="Getaccesstoken">
        1. Authenticate successfully in your app<br />
        2. Input your SSOID got from target system admin<asp:textbox runat="server" name="ssoid" id="idtext" value="testid" />
           <asp:button runat="server" id="btnSend" Text="Get an accessToken from target system" OnClick="btnSend_Click"></asp:button><br />
        3. User Account<asp:textbox runat="server" name="ssoid" id="account" value="account_test" />
           Use accesskey got from target system admin and encrypt user account<asp:textbox runat="server" name="ssoid" id="accesskey" value="12345678901234567890123456789012" size="35" /><br />
           After calculating,
           get userToken<asp:button runat="server" id="Button1" Text="Encrypt user account to get userToken" OnClick="btnButton1_Click"></asp:button>
        <asp:HiddenField runat="server" ID="aspid" />
        <asp:HiddenField runat="server" ID="aspaccesstoken" />
        <asp:HiddenField runat="server" ID="aspusertoken" />
    </form>

    <form action="http://localhost:2695/SSOLogin.aspx" method="post" id="submitform">
    4. Submit id, accesstoken, usertoken via http POST command to the target system
    <input type="hidden" name="id" id="Text1" value="<%=aspid.Value %>" /><br />
    AccessToken:<input type="text" id="accessToken" name="accessToken" size=100 value="<%=aspaccesstoken.Value %>" /><br />
    usertoken<input type="text" name="userToken" size="40" value="<%=usertoken %>" /><br />
    <input type="submit" id="submitbtn" value="submit to target system and login" />
</form>
</body>

</html>
