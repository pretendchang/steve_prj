
#include "../nc_controller.h"
#include "plugin2.h"
#include <stdio.h>

struct protocol_command protocol_command_rtsp[] = 
{
	{260, toipcam_request_rtsp_260, toipcam_ack_rtsp_260, cam_send_rtsp_260, cam_resp_rtsp_260},
	{261, toipcam_request_rtsp_261, toipcam_ack_rtsp_261, cam_send_rtsp_261, cam_resp_rtsp_261}
};

int system_init_rtsp();
int entry_point_rtsp(int channel);
int system_free_rtsp();
int read_config_rtsp();
int listen_packetrtsp();
int handle_command_rtsp(struct nc_controller_protocol *protocol, void *camcom);
Protocol_Plugin struct nc_controller_protocol protocol_rtsp = 
{
	system_init_rtsp,
	entry_point_rtsp,
	system_free_rtsp,
	read_config_rtsp,
	0,
	handle_command_rtsp,
	&protocol_command_rtsp,
	2,
	"rtsp"
};

int system_init_rtsp()
{
	int index;
	printf("system_init_rtsp\n");
	index = find_idle_channel(&protocol_rtsp, 1);
	ncclient_session(index);
	return 1;
}
int entry_point_rtsp(int channel)
{
	int cmdid=260;
	printf("entry_point_rtsp\n");
	handle_ipcam_command(channel,(void*)&cmdid);
	return 1;
}
int system_free_rtsp()
{
	printf("system_free_rtsp\n");
	return 1;
}
int read_config_rtsp()
{
	printf("read_config_rtsp\n");
	return 1;
}

int handle_command_rtsp(struct nc_controller_protocol *protocol, void *camcom)
{
	int cmdid,i;
	struct protocol_command *command;
	cmdid = *(int*)camcom;
	printf("handle_command_rtsp\n");
	for(i=0;i<protocol->command_count;i++)
	{
		if(protocol->command[i].commandid==cmdid)
		{
			command = &protocol->command[i];
			break;
		}
	}
	command->toipcam_request();
	command->toipcam_ack();
	return 1;
}