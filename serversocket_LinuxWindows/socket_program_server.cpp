// socket_program.cpp : 定義主控台應用程式的進入點。
//

// ConsoleApplication2.cpp : 定義主控台應用程式的進入點。
//

#ifdef WIN32
	#include "stdafx.h"
	#include<windows.h>
#else
#include <unistd.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#endif
#include<errno.h>
#include<stdio.h>
#include<string.h>

#ifdef WIN32
	#define socklen_t int
#else
	#define SOCKET unsigned short
    #define closesocket(fd) close(fd)
#endif

#define LISTENING_PORT 515
int streamhandling(char *data, int datalen)
{
	FILE *evtlog;
	char stmp[100];
	int i;
	evtlog = fopen("eve.log","a");
	data[datalen]=0;
	fwrite(data,1,datalen,evtlog);
	for(i=0;i<datalen;i++)
	{
		printf("%d ",(int)data[i]);
	}printf("\n");

	fclose(evtlog);
	return 1;
}

int recvClientStream(SOCKET acceptSocket)
{
#define BUFFLEN 10240
	fd_set recvfd;
	struct timeval tv;
	char data[BUFFLEN];
	int i=0,selret=0;
	tv.tv_sec = 60;		
	tv.tv_usec = 0;

	while(1)
	{
		FD_ZERO(&recvfd);
		FD_SET(acceptSocket, &recvfd);
		tv.tv_sec = 60;		
		tv.tv_usec = 0;
		if((selret=select(acceptSocket+1, &recvfd, NULL, NULL, &tv))<0)
		{
			printf("select read error:%d %s\n",selret,strerror(errno));
			return 0;
		}
		else if(selret==0)
		{
			printf("timeout\n");
#ifdef WIN32
			Sleep(1000);
#else
			sleep(1);
#endif
			continue;
		}
		int res = recv(acceptSocket, data, BUFFLEN, 0);
		if(res>0)
		{
			streamhandling(data, res);
			send(acceptSocket, data, res, 0);
		}
		else
		{
			printf("recv error:%d %s\n",res,strerror(errno));
			break;
		}
	}
	return 1;
}

int socketentry(char *argc)
{
	SOCKET sockfd;
#ifdef WIN32		
	WSADATA wsaData;
#endif	
	struct sockaddr_in dest_addr;
	struct sockaddr client_addr;
	fd_set readfd;
	struct timeval tv;
	int listenport=0;
	tv.tv_sec = 60;		
	tv.tv_usec = 0;
	if(strcmp(argc,"a")==0)
	{
		listenport=513;
	}
	else
		listenport=515;
	printf("listen:%d\n",listenport);
#ifdef WIN32		
	WSAStartup(0x202, &wsaData);
#endif
	sockfd=socket(PF_INET,SOCK_STREAM, 0);
	
	memset(&dest_addr, 0, sizeof(dest_addr));
#ifdef WIN32	
	dest_addr.sin_addr.S_un.S_addr=INADDR_ANY;
#else
	dest_addr.sin_addr.s_addr=INADDR_ANY;
#endif
	dest_addr.sin_family=AF_INET;
	dest_addr.sin_port=htons(listenport);
	
	bind(sockfd, (struct sockaddr*)&dest_addr, sizeof(dest_addr));
	listen(sockfd, listenport);

	while(1)
	{
		///重置fd_set和timeout
		///select會更改fd_set和timeout參數的內容，因此每次執行前必須重置這些參數
		FD_ZERO(&readfd);
		FD_SET(sockfd, &readfd);
		tv.tv_sec = 60;		
		tv.tv_usec = 0;
		///select是否有fd發生事件
		///sockfd+1表示，檢查等待fd值在sockfd+1以下，且使用fd_set設定過的fd是否有事件發生
		if(select(sockfd+1, &readfd, NULL, NULL, &tv)<=0)
		{///select <0時為發生錯誤，select=0代表timeout 
			printf("select error:%s\n",strerror(errno));
#ifdef WIN32
			Sleep(1000);
#else
			sleep(1);
#endif
			continue;
		}
		///某個fd有網路封包傳入，檢查哪個fd
		if(FD_ISSET(sockfd, &readfd))
		{
			socklen_t addrlen = sizeof(client_addr);
			///某個client連線進來，取得該client的acceptfd和address
			SOCKET acceptFD = accept(sockfd, &client_addr, &addrlen);
			///處理該client的網路交握訊息
			recvClientStream(acceptFD);
			///處理完畢，將該acceptfd關閉
			closesocket(acceptFD);
			printf("closeacceptfd\n");
		}
		//printf("while\n");
	}
	///關閉listen fd
	closesocket(sockfd);
#ifdef WIN32		
	WSACleanup();
#endif	
	return 0;
}

#ifndef WIN32
int main(int argc, char *argv[])
{
	socketentry(argv[1]);
    return 1;
} /* main */
#else
int _tmain(int argc, _TCHAR* argv[])
{
	return 0;
}
#endif



