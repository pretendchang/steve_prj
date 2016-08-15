---
title: cross windows & linux plateform basic socket program
---
## socket通訊程式
大部分的socket通訊程式都有一個需求---接收對方傳送的資料，這個需求實作的好壞，影響了系統的效率
socket通訊程式接收資料最直覺的做法是block io，recv函數在沒收到資料的情況下，會持續等待資料這使得程式停住無法做其他事情，效率不彰
此議題衍生出non-block io的模式，recv函數即使沒有收到資料情況下仍會立刻回傳，因此程式必須時時呼叫recv函數檢查是否有資料進來，但這讓程式花費額外cpu去檢查
為了解決這問題，socket提供select函數，當有封包進入程式，select函式通知哪個socket有資料傳入，這時再去處理這個socket，把檢查資料進來的工作交由kernel處理
這篇文章將以select方式實作一個跨windows和linux平台的簡易網路通訊程式

## select函數說明
```cs
int select(int nfds, fd_set *readfds, fd_set *writefds, fd_set *exceptfds, struct timeval *timeout);
```
第一個參數在windows平台是沒意義的，可為任意整數，linux平台是後面三組fd_set參數中所包含的最大fd的數值加1
第二、第三和第四個參數分別是監控fd是否有讀取、寫入或例外事件發生，若有事件發生select回傳這三個fd_set所監聽的總fd數目，fd_set可由FD_SET(fd)或FD_CLR(fd)兩個macro將fd設定或自fd_set移除
第五個參數是select監聽的最長時間，若在這時間內，完全沒有事件發生，select回傳0
其中第二、三、四、五參數，指標指向的值有可能會被select函數更改，因此使用select函數需處理以下需求
1. 每一個loop cycle要傳入目前要接收的fd_set
2. 若有新加入fd要接收或是fd斷線關閉，須將其加入fd_set或至fd_set移除
3. select在linux平台第一個參數是目前等待事件發生的所有fd中最大值加1，因此在linux平台需更新這個數值
4. 每次呼叫select前須重新設定timeval


## 使用select注意的地方
由於select會改變recvfd的值，因此需另外定義masterfd來儲存目前建立連線的fd，每次呼叫select前都要設定fd_set為masterfd和timeval
```cs
recvfd=masterfd;		
tv.tv_sec = 3;		
tv.tv_usec = 0;
selret=select(maxsockfd+1, &recvfd, NULL, NULL, &tv)
```

每次建立連線成功後須更新maxsockfd和fd_set
```cs
ret = connect(sockfd[i], (struct sockaddr *) &dest_addr[i], sizeof(dest_addr[i]));
if(ret<0)
{
	printf("connect error:%s\n",strerror(errno));
}
else
{			
	set_maxsockfd(sockfd[i]);			
	FD_SET(sockfd[i], &masterfd);
}
```

關閉連線前須更新maxsockfd和fd_set
```cs
int res = recv(sockfd[i], data, BUFFLEN, 0);
if(res>0)
{
	//recv successfully
}
else
{
	//recv fail
	FD_CLR(sockfd[i], &masterfd);
	remove_maxsockfd(sockfd[i]);
	closesocket(sockfd[i]);
}
```

## windows和linux平台的差異
### socket
windows平台和linux平台在socket實作，最大的差別有2
1. windows平台需要初始化WSDATA，最後不使用socket前，須把WSDATA資源釋放
```cs
#ifdef WIN32		
	WSADATA wsaData;
#endif	

#ifdef WIN32		
	WSAStartup(0x202, &wsaData);
#endif

#ifdef WIN32		
	WSACleanup();
#endif	
```
2. select的第一個參數目前等待事件發生的所有fd中最大值加1
每次建立連線時或斷線後，程式需分別呼叫set_maxsockfd、remove_maxsockfd以更新maxsockfd的值
```cs
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
```
3. 呼叫select時，即可使用maxsockfd，由於windows平台不使用這個參數，可填任意值，因此我在Windows平台仍保留maxsockfd這變數
```cs
select(maxsockfd+1, &recvfd, NULL, NULL, &tv)
```

另還有兩個較小的差異
1. 連線fd的部分，windows平台定義了SOCKET結構封裝，我用了windows平台的SOCKET結構，將linux的fd包裝起來關閉連線的部分，windows平台函數名稱為closesocket，可讀性較高，因此採用windows平台的命名將linux的close封裝起來
```cs
 #define SOCKET unsigned short
#define closesocket(fd) close(fd)
```
2. 設定連線ip的部分，也有些微的不同
```cs
#ifdef WIN32	
		dest_addr[i].sin_addr.S_un.S_addr=INADDR_ANY;
#else
		dest_addr[i].sin_addr.s_addr=INADDR_ANY;
#endif

#ifdef WIN32	
		InetPton(AF_INET, (PCWSTR)L"192.168.0.1", &(dest_addr[i].sin_addr));
#else
		inet_pton(AF_INET, "192.168.0.1", &(dest_addr[i].sin_addr));
#endif
```

### pthread

執行序的部分，兩個平台在結構名稱和函數名稱都不同，幸運地是每個函數和結構都可以一對一對應，封裝的難度不高
```cs
#ifdef WIN32
	typedef unsigned long Thread_t;
	typedef CRITICAL_SECTION Thread_Mutex_t;
	typedef CONDITION_VARIABLE Cond_t;
#else
	typedef pthread_t Thread_t;
	typedef pthread_mutex_t Thread_Mutex_t;
	typedef pthread_cond_t Cond_t;
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
```