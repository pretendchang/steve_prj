// socket_program_client.cpp : 定義主控台應用程式的進入點。
//


#ifdef WIN32
	#include "stdafx.h"
	#include<winsock2.h>
	#include<Ws2tcpip.h>
#else
#include <unistd.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <pthread.h>
#endif
#include<errno.h>
#include<stdio.h>
#include<string.h>

#ifdef WIN32
	#define socklen_t int
	#define MSG_NOSIGNAL 0
	typedef unsigned long Thread_t;
	typedef CRITICAL_SECTION Thread_Mutex_t;
	typedef CONDITION_VARIABLE Cond_t;
#else
	#define SOCKET unsigned short
    #define closesocket(fd) close(fd)
	typedef pthread_t Thread_t;
	typedef pthread_mutex_t Thread_Mutex_t;
	typedef pthread_cond_t Cond_t;
#endif
#define sockemax 2
SOCKET sockfd[sockemax];

struct sockaddr_in dest_addr[sockemax];
int clientstatus[sockemax]={0,0};

Thread_Mutex_t socket_cond_mutex[sockemax];
Cond_t  socket_cond[sockemax];
fd_set masterfd;


SOCKET maxsockfd=0;
#ifndef WIN32
void set_maxsockfd(SOCKET s)
{
	maxsockfd = ((s > maxsockfd)?s:maxsockfd);
}
void remove_maxsockfd(SOCKET s)
{
	int i;
	if(s > maxsockfd)
	{
		maxsockfd=0;
		for(i=0;i<sockemax;i++)
		{
			if(clientstatus[i]==1)
				set_maxsockfd(sockfd[i]);
		}
	}
}
#endif


void thread_create(void *fp, void *args, Thread_t *t)
{
#ifdef WIN32
	CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE)fp, args, 0, NULL);
#else
	pthread_create (t, NULL, fp, args);
#endif
}

void cond_init(Cond_t *pCond)
{
#ifdef WIN32
	InitializeConditionVariable((PCONDITION_VARIABLE)pCond);
#else
	pthread_cond_init(pCond, NULL);
#endif
}

void mutex_init(Thread_Mutex_t *pMutex)
{
#ifdef WIN32
	InitializeCriticalSection((PCRITICAL_SECTION)pMutex);
#else
	pthread_mutex_init(pMutex, NULL);
#endif
}

void mutex_lock(Thread_Mutex_t *pMutex)
{
#ifdef WIN32
	EnterCriticalSection((PCRITICAL_SECTION)pMutex);
#else
	pthread_mutex_lock(pMutex);
#endif
}

void mutex_unlock(Thread_Mutex_t *pMutex)
{
#ifdef WIN32
	LeaveCriticalSection((PCRITICAL_SECTION)pMutex);
#else
	pthread_mutex_unlock(pMutex);
#endif
}

void connthread()
{
	int ret=0,i;
	while(1)
	{
		for(i=0;i<sockemax;i++)
		{
			if(clientstatus[i]!=0)
			{
#ifdef WIN32
			Sleep(1000);
#else
			sleep(1);
#endif
				continue;
			}
			sockfd[i]=socket(PF_INET,SOCK_STREAM, 0);
			ret = connect(sockfd[i], (struct sockaddr *) &dest_addr[i], sizeof(dest_addr[i]));
			if(ret<0)
			{
				printf("connect error:%s\n",strerror(errno));
#ifdef WIN32
			Sleep(1000);
#else
			sleep(1);
#endif
			}
			else
			{
				mutex_lock(&socket_cond_mutex[i]);
				//pthread_cond_broadcast(&socket_cond[i]);
				clientstatus[i]=1;
#ifndef WIN32				
				set_maxsockfd(sockfd[i]);
#endif				
				FD_SET(sockfd[i], &masterfd);
				mutex_unlock(&socket_cond_mutex[i]);
				///該sockfd連線成功將其加入fd_set中
				
				printf("connect successful\n");
			}
		}
	}
}

int streamhandling(char *data, int datalen)
{
	FILE *evtlog;
	char stmp[100];
	int i;
	evtlog = fopen("eve.log","a");
	data[datalen]=0;
	fwrite(data,1,datalen,evtlog);
	printf("recv:%s\n",data);fflush(stdout);
	fclose(evtlog);
	return 1;
}
#define BUFFLEN 1024
int socketentry()
{
	Thread_t tidConnectRelay;
	int sendcount=0,icon=0, selret=0,i;
	char scon[BUFFLEN];
	
	fd_set recvfd;
	struct timeval tv;
	char data[BUFFLEN];
	
	for(i=0;i<sockemax;i++)
	{
		mutex_init(&socket_cond_mutex[i]);
		cond_init(&socket_cond[i]);
	}

#ifdef WIN32		
	WSADATA wsaData;
#endif	

#ifdef WIN32		
	WSAStartup(0x202, &wsaData);
#endif
	for(i=0;i<sockemax;i++)
	{
		memset(&dest_addr[i], 0, sizeof(dest_addr[i]));

		dest_addr[i].sin_family=AF_INET;
		dest_addr[0].sin_port=htons(513);
		dest_addr[1].sin_port=htons(515);
#ifdef WIN32	
		InetPton(AF_INET, (PCWSTR)L"192.168.146.133", &(dest_addr[i].sin_addr));
#else
		inet_pton(AF_INET, "192.168.13.77", &(dest_addr[i].sin_addr));
#endif
	}
	thread_create(connthread, NULL, &tidConnectRelay);
	//pthread_create (&tidReadRelay, NULL, recvClientStream, NULL);	
	while(1)
	{
		for(i=0;i<sockemax;i++)
		{
			if(clientstatus[i]==0)//unconnected
			{
			//	pthread_mutex_lock(&socket_cond_mutex[i]);
			//	pthread_cond_wait(&socket_cond[i], &socket_cond_mutex[i]);
			//	pthread_mutex_unlock(&socket_cond_mutex[i]);
				continue;
			}printf("%d %d\n",i,clientstatus[i]);
			if(clientstatus[i]==1)
			{
				sprintf(scon,"%03d",icon);
				sendcount = send(sockfd[i], scon, strlen(scon), MSG_NOSIGNAL);
				printf("main send:%d\n",sendcount);fflush(stdout);
			}
		}
				
		///讀取server的回應
		{			
			///重置fd_set和timeout
			///select會更改fd_set和timeout參數的內容，因此每次執行前必須重置這些參數
			recvfd=masterfd;
			
			tv.tv_sec = 3;		
			tv.tv_usec = 0;
			if((selret=select(maxsockfd+1, &recvfd, NULL, NULL, &tv))<0)
			{
				printf("select error:%s\n",strerror(errno));
				for(i=0;i<sockemax;i++)
				{
					mutex_lock(&socket_cond_mutex[i]);
					clientstatus[i]=0;
					mutex_unlock(&socket_cond_mutex[i]);
					///select失敗將所有sockfd自fd_set中移除
					FD_CLR(sockfd[i], &masterfd);
#ifndef WIN32
					remove_maxsockfd(sockfd[i]);
#endif
					closesocket(sockfd[i]);
				}
			}
			else if(selret==0)
			{
				printf("select timeout\n");
#ifdef WIN32
			Sleep(1000);
#else
			sleep(1);
#endif
				continue;
			}
			for(i=0;i<sockemax;i++)
			{
				if(FD_ISSET(sockfd[i], &recvfd))
				{
					int res = recv(sockfd[i], data, BUFFLEN, 0);
					printf("get something:%d\n",res);fflush(stdout);
					if(res>0)
					{
						streamhandling(data, res);
						//send(acceptSocket, data, res, 0);
					}
					else
					{
						mutex_lock(&socket_cond_mutex[i]);
						clientstatus[i]=0;
						mutex_unlock(&socket_cond_mutex[i]);
						///網路封包recv失敗，該sockfd自fd_set中移除
						FD_CLR(sockfd[i], &masterfd);
#ifndef WIN32
						remove_maxsockfd(sockfd[i]);
#endif
						closesocket(sockfd[i]);
					}
				}
			}
			
		}
#ifdef WIN32
			Sleep(1000);
#else
			sleep(1);
#endif
		icon++;
	}
	for(i=0;i<sockemax;i++)
	{
		closesocket(sockfd[i]);
	}
#ifdef WIN32		
	WSACleanup();
#endif	
	return 0;
}

#ifndef WIN32
int main(int argc, char *argv[])
{
	socketentry();
    return 1;
} /* main */
#else
int _tmain(int argc, _TCHAR* argv[])
{
	socketentry();
	return 0;
}
#endif

