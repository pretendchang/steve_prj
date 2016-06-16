

#include<sys/stat.h>
#include<unistd.h>
#include<stdio.h>
#include<dirent.h>
#include<string.h>

#include<time.h>
#include<stdlib.h>
#include<pthread.h>
#include<errno.h>
#include<regex.h>
#include<limits.h>
typedef struct _space_check
{
	long _time_duration;
	long _space_threashold;
	char _file_expired_checked_path[100];
}space_check;

typedef struct _traveldir_breakcondition traveldir_breakcondition;
typedef struct _traveldir_breakcondition
{
	char *dayexpiredpath;
	char *timeexpiredpath;
};//for customized struct

typedef struct _traveldir_handler traveldir_handler;
typedef struct _traveldir_handler
{
	int (*Filter)(const struct dirent *);
	int (*compar)(const struct dirent **, const struct dirent **);
	int (*handling)(const char *filepath, const traveldir_handler *handler, const traveldir_breakcondition* breakchecker);
	int (*breakcondition)(const char *filename, const int ret_handling, const traveldir_breakcondition*);
	traveldir_handler *nexthandler;
};//the definition can't modified


int traveldir(const char *path, struct dirent ***namelist, 
              const traveldir_handler *handler, const traveldir_breakcondition *breakchecker);

void * deletefilethread(void* arg);
#define ETESTERROR 1000

char * errordesc[]={
	[0]="abc\0",
	[1]="def\0",
	[2]="ytr\0",
};


char *custom_strerror(int error)
{
	if(error >= ETESTERROR)
	{
		return errordesc[error-ETESTERROR];
	}
	else
		return strerror(error);
}



