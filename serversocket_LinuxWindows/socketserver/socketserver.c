

#include <unistd.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <unistd.h>
#include <fcntl.h>
#include<errno.h>
#include<stdio.h>
#include<string.h>


#define SOCKET unsigned short
#define closesocket(fd) close(fd)

#define _PORT_ 513
#define MAXacceptSOCKETFD 100
SOCKET maxsockfd;
SOCKET acceptsockfd[MAXacceptSOCKETFD];
int sockemax=0;
void set_maxsockfd(SOCKET s)
{
	maxsockfd = ((s > maxsockfd)?s:maxsockfd);
}

void addSocketFD(SOCKET s)
{
	acceptsockfd[sockemax++]=s;
	set_maxsockfd(s);
}

int streamhandling(char *data, int datalen)
{
	char stmp[100];
	int i;

	data[datalen]=0;

	for(i=0;i<datalen;i++)
	{
		printf("%d ",(int)data[i]);
	}printf("\n");


	return 1;
}

int socketentry(char *argc)
{
	SOCKET sockfd;

	struct sockaddr_in dest_addr;
	struct sockaddr client_addr;
	fd_set readfd;
	struct timeval tv;
	int listenport=0;
	tv.tv_sec = 60;		
	tv.tv_usec = 0;
	listenport=_PORT_;

	printf("listen:%d\n",listenport);

	sockfd=socket(PF_INET,SOCK_STREAM, 0);
	
	memset(&dest_addr, 0, sizeof(dest_addr));

	dest_addr.sin_addr.s_addr=INADDR_ANY;

	dest_addr.sin_family=AF_INET;
	dest_addr.sin_port=htons(listenport);
	
	bind(sockfd, (struct sockaddr*)&dest_addr, sizeof(dest_addr));
	set_maxsockfd(sockfd);printf("sockfd=%d, maxfd=%d\n",sockfd,maxsockfd);
	listen(sockfd, listenport);

	while(1)
	{
		int i;
		FD_ZERO(&readfd);
		FD_SET(sockfd, &readfd);
		for(i=0; i<sockemax; i++)
		{
			FD_SET(acceptsockfd[i], &readfd);
		}
		tv.tv_sec = 60;		
		tv.tv_usec = 0;

		if(select(maxsockfd+1, &readfd, NULL, NULL, &tv)<=0)
		{
			printf("select error:%s\n",strerror(errno));

			sleep(1);

			continue;
		}printf("maxfd=%d\n",maxsockfd);

		if(FD_ISSET(sockfd, &readfd))
		{
			socklen_t addrlen = sizeof(client_addr);

			SOCKET acceptFD = accept(sockfd, &client_addr, &addrlen);
			fcntl(acceptFD, F_SETFL, O_NONBLOCK);
			addSocketFD(acceptFD);printf("==========================accept connected\n");printf("acceptFD=%d, maxfd=%d\n",acceptFD,maxsockfd);
		}
#define BUFFLEN 10240
		char data[BUFFLEN];printf("sockemax=%d\n",sockemax);
		for(i=0; i<sockemax; i++)
		{
			if(FD_ISSET(acceptsockfd[i], &readfd))
			{
				int res = recv(acceptsockfd[i], data, BUFFLEN, 0);
				if(res>0)
				{
					streamhandling(data, res);
					send(acceptsockfd[i], data, res, 0);
				}
				else
				{
					printf("recv error[%d]:%d %s\n",i,res,strerror(errno));
					sleep(1);
				}
			}
		}
	}

	closesocket(sockfd);
	
	return 0;
}


int main(int argc, char *argv[])
{
	socketentry(argv[1]);
    return 1;
} /* main */
