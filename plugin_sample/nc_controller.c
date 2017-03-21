
#include "nc_controller.h"
#include <stdio.h>
#include <string.h>
struct client_info
{
	int acceptfd;
	struct nc_controller_protocol *protocol;
};
struct client_info client[10];

extern char _protocol_module_start;
extern char _protocol_module_end;
int protocol_size = 0;

int load_protocols()
{
	int i;
	struct nc_controller_protocol *protocol = (struct nc_controller_protocol *)&_protocol_module_start;
	protocol_size = ((int)&_protocol_module_end - (int)&_protocol_module_start)/sizeof(struct nc_controller_protocol);

	for(i=0; i<protocol_size; i++,protocol++)
	{
		printf("Load Plugin %s\n",protocol->name);
	}
	return 1;
}

struct nc_controller_protocol * hook_protocol_plugin(char *plugin_name)
{
	int i;
	struct nc_controller_protocol *protocol = (struct nc_controller_protocol *)&_protocol_module_start;
	for(i=0; i<protocol_size; i++,protocol++)
	{
		if(strcmp(protocol->name, plugin_name)==0)
			return protocol;
	}
	printf("hook_protocol_plugin:find nothing:%s\n",plugin_name);
	return 0;
}

int system_init()
{
	int i;
	struct nc_controller_protocol *protocol = (struct nc_controller_protocol *)&_protocol_module_start;
	for(i=0; i<protocol_size; i++,protocol++)
	{
		protocol->system_init();
	}
	return 1;
}

int system_free()
{
	int i;
	struct nc_controller_protocol *protocol = (struct nc_controller_protocol *)&_protocol_module_start;
	for(i=0; i<protocol_size; i++,protocol++)
	{
		protocol->system_free();
	}
	return 1;
}

int read_config(char * file)
{
	int i;
	struct nc_controller_protocol *protocol = (struct nc_controller_protocol *)&_protocol_module_start;
	for(i=0; i<protocol_size; i++,protocol++)
	{
		protocol->read_config();
	}
	return 1;
}

int listen_packet()
{
	int i;
	struct nc_controller_protocol *protocol = (struct nc_controller_protocol *)&_protocol_module_start;
	for(i=0; i<protocol_size; i++,protocol++)
	{
		if(protocol->listen_packet != 0)
			protocol->listen_packet();
	}
	return 1;
}

int ncclient_session(int index)
{
	printf("ncclient_session index:%d\n",index);
	client[index].protocol->entry_point(index);
	return 1;
}
int handle_ipcam_command(int index, void *camcom)
{
	printf("handle_ipcam_command\n");
	client[index].protocol->handle_command(client[index].protocol, camcom);
	return 1;
}
int find_idle_channel(struct nc_controller_protocol *protocol, int index)
{
	client[index].protocol = protocol;
	return index;
}
int api1()
{
	printf("executing api1\n");
	return 1;
}
int api2()
{
	printf("executing api2\n");
	return 1;
}
int api3()
{
	printf("executing api3\n");
	return 1;
}
int api4()
{
	printf("executing api4\n");
	return 1;
}
int main()
{
	load_protocols();
	read_config("test");
	system_init();
	printf("================simulate a client\n");
	listen_packet();
	printf("================end simulate a client\n");
	system_free();
	return 1;
}