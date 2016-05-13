#ifdef _PORTEUS_64
#define __FSWORD_T_TYPE int
#define __SYSCALL_SLONG_TYPE long int
#define __SYSCALL_ULONG_TYPE unsigned long int
#endif
#ifdef WIN32
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

int streamhandling(char *data, int datalen)
{
	FILE *evtlog;
	char stmp[100];
	int i;
	evtlog = fopen("eve.log","a");
	data[datalen]=0;
	fwrite(data,1,datalen,evtlog);

	fclose(evtlog);
	return 1;
}

int recvClientStream(SOCKET acceptSocket)
{
#define BUFFLEN 10240
	fd_set recvfd;
	struct timeval tv;
	char data[BUFFLEN];
	int i=0;
	tv.tv_sec = 60;		
	tv.tv_usec = 0;
	FD_ZERO(&recvfd);
	FD_SET(acceptSocket, &recvfd);
	while(1)
	{
		if(select(acceptSocket+1, &recvfd, NULL, NULL, &tv)<=0)
			return 0;
		int res = recv(acceptSocket, data, BUFFLEN, 0);
		if(res>0)
		{
			streamhandling(data, res);
			//send(acceptSocket, data, res, 0);
		}
		else
			break;
	}
	return 1;
}

int socketentry()
{
	SOCKET sockfd;
#ifdef WIN32		
	WSADATA wsaData;
#endif	
	struct sockaddr_in dest_addr;
	struct sockaddr client_addr;
	fd_set readfd;
	struct timeval tv;
	tv.tv_sec = 60;		
	tv.tv_usec = 0;
	FD_ZERO(&readfd);
#ifdef WIN32		
	WSAStartup(0x202, &wsaData);
#endif
	sockfd=socket(PF_INET,SOCK_STREAM, 0);
	FD_SET(sockfd, &readfd);
	memset(&dest_addr, 0, sizeof(dest_addr));
#ifdef WIN32	
	dest_addr.sin_addr.S_un.S_addr=INADDR_ANY;
#else
	dest_addr.sin_addr.s_addr=INADDR_ANY;
#endif
	dest_addr.sin_family=AF_INET;
	dest_addr.sin_port=htons(514);
	
	bind(sockfd, (struct sockaddr*)&dest_addr, sizeof(dest_addr));
	listen(sockfd, 514);

	while(1)
	{
		if(select(sockfd+1, &readfd, NULL, NULL, &tv)<=0)
		{
			//printf("continue\n");
			continue;
		}

		if(FD_ISSET(sockfd, &readfd))
		{
			socklen_t addrlen = sizeof(client_addr);
			SOCKET acceptFD = accept(sockfd, &client_addr, &addrlen);
			recvClientStream(acceptFD);
			closesocket(acceptFD);
		}
		//printf("while\n");
	}
	closesocket(sockfd);
#ifdef WIN32		
	WSACleanup();
#endif	
	return 0;
}


int main(int argc, char *argv[])
{
	socketentry();
    return 1;
} /* main */
