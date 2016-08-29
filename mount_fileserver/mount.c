#ifndef __FSWORD_T_TYPE
	#define __FSWORD_T_TYPE int
#endif
#ifndef __SYSCALL_SLONG_TYPE
	#define __SYSCALL_SLONG_TYPE long int
#endif
#ifndef __SYSCALL_ULONG_TYPE
	#define __SYSCALL_ULONG_TYPE unsigned long int
#endif

#include <mntent.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <stdio.h>

#include <unistd.h>
#include <arpa/inet.h>
#include <sys/socket.h>
int CheckMount(char* mountDir)
{
	int isMounted=0;
	char logData[128];
	struct mntent* mntInfo;
	FILE *fp=0;

	fp = setmntent("/etc/mtab", "r");

	if (fp == NULL)
	{
		sprintf(logData, "Open /etc/mtab error. (%s)\n", strerror(errno));
		printf(logData);
		return 0;
	}

	while ((mntInfo = getmntent(fp)) != NULL)
	{
		if (!strcmp(mntInfo->mnt_dir, mountDir))
			isMounted = 1;
	}

	endmntent(fp);
	return isMounted;
}

int probe_remote_protocol(char *ip, int port)
{
	int socketfd, ret;
	struct sockaddr_in dest_addr;
	socketfd = socket(AF_INET, SOCK_STREAM, 0);
	
	dest_addr.sin_family=AF_INET;
	dest_addr.sin_port=htons(port);
	inet_pton(AF_INET, ip, &(dest_addr.sin_addr));
	
	ret = connect(socketfd, (struct sockaddr *) &(dest_addr), sizeof(dest_addr));
	close(socketfd);
	
	if(ret <0)
		return 0;
	else
		return 1;
}

int mount_remote_fileserver(char *ip, char *mountpoint)
{
	char sysCmd[512];
	char *ncPath=mountpoint;

	if(probe_remote_protocol(ip, 111)==1)
	{//nfs
		printf("nfs host found\n");
		sprintf(sysCmd, "mount -t nfs -o ro,soft,tcp,rsize=32768,wsize=32768,timeo=5,intr,nolock,actimeo=1 %s:/disk1 %s\n", ip, ncPath);
		printf("exec:%s\n",sysCmd);fflush(stdout);
		system(sysCmd);

		if (CheckMount(ncPath) == 0)
		{
			printf("mount error:%s\n",sysCmd);fflush(stdout);
		}
	}
	else if(probe_remote_protocol(ip, 445)==1)
	{//cifs
		printf("cifs host found\n");
		printf("exec2:%s\n",sysCmd);fflush(stdout);
		sprintf(sysCmd, "mount -t cifs -o username=\"%s\",password=\"%s\" //%s/disk1 %s\n", "stevechang","Good4sysmgnt",ip, ncPath);
		system(sysCmd);
		if (CheckMount(ncPath) == 0)
		{
			printf("mount error:%s\n",sysCmd);fflush(stdout);
		}
	}
	else
	{
		//unknown or host off line
		printf("no host found\n");
	}
}

int main(int argc, char *argv[])
{
	if(argv[1]==NULL)
	{
		printf("usage:mount ip\n");
		return 1;
	}
	mount_remote_fileserver(argv[1],"/disk1/usb2");
	return 1;
} /* main */
