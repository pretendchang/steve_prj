
#define Protocol_Plugin __attribute__((section(".protocol_module")))

struct protocol_command
{
	int commandid;
	int (*toipcam_request)();
	int (*toipcam_ack)();
	int (*cam_send)();
	int (*cam_resp)();
};

struct nc_controller_protocol 
{
	int (*system_init)();
	int (*entry_point)(int index);
	int (*system_free)();
	int (*read_config)();
	int (*listen_packet)();
	int (*handle_command)(struct nc_controller_protocol *protocol, void* camcom);
	struct protocol_command *command;
	char command_count;
	char name[7];
};

int ncclient_session(int index);
int handle_ipcam_command(int index,void *camcom);
int find_idle_channel(struct nc_controller_protocol *protocol, int index);
int api1();
int api2();
int api3();
int api4();