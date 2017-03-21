
#include "../nc_controller.h"
#include "plugin1.h"
#include <stdio.h>

struct protocol_command protocol_command_ms8101[] = 
{
	{512, toipcam_request_ms8101_512, toipcam_ack_ms8101_512, cam_send_ms8101_512, cam_resp_ms8101_512},
	{513, toipcam_request_ms8101_513, toipcam_ack_ms8101_513, cam_send_ms8101_513, cam_resp_ms8101_513}
};

int system_init_ms8101();
int entry_point_ms8101(int channel);
int system_free_ms8101();
int read_config_ms8101();
int listen_packet_ms8101();
int handle_command_ms8101(struct nc_controller_protocol *protocol, void *camcom);
Protocol_Plugin struct nc_controller_protocol protocol_ms8101 = 
{
	system_init_ms8101,
	entry_point_ms8101,
	system_free_ms8101,
	read_config_ms8101,
	listen_packet_ms8101,
	handle_command_ms8101,
	&protocol_command_ms8101,
	2,
	"ms8101"
};
struct _camcom
{
	int cmdid;
	int input;
};
int system_init_ms8101()
{
	printf("system_init_ms8101\n");
	return 1;
}
int entry_point_ms8101(int channel)
{
	struct _camcom cmd;
	cmd.cmdid=512;cmd.input=0;
	printf("entry_point_ms8101\n");
	handle_ipcam_command(channel, (void*)&cmd);
	return 1;
}
int system_free_ms8101()
{
	printf("system_free_ms8101\n");
	return 1;
}
int read_config_ms8101()
{
	printf("read_config_ms8101\n");
	return 1;
}
int listen_packet_ms8101(char *a)
{
	int index=0;
	printf("listen_packet_ms8101\n");

	index = find_idle_channel(&protocol_ms8101, 0);
	ncclient_session(index);
	return 1;
}
int handle_command_ms8101(struct nc_controller_protocol *protocol, void *camcom)
{
	int i;
	struct _camcom *cmd;
	struct protocol_command *command;
	cmd = (struct _camcom*)camcom;
	printf("handle_command_ms8101\n");
	for(i=0;i<protocol->command_count;i++)
	{
		if(protocol->command[i].commandid==cmd->cmdid)
		{
			command = &protocol->command[i];
			break;
		}
	}
	command->toipcam_request();
	command->toipcam_ack();
	command->cam_send();
	command->cam_resp();
	return 1;
}