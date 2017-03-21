
#include "../nc_controller.h"
#include <stdio.h>
int toipcam_request_ms8101_512()
{
	printf("toipcam_request_ms8101_512\n");
	api1();
	return 1;
}
int toipcam_ack_ms8101_512()
{
	printf("toipcam_ack_ms8101_512\n");
	return 1;
}
int cam_send_ms8101_512()
{
	printf("cam_send_ms8101_512\n");
	return 1;
}
int cam_resp_ms8101_512()
{
	printf("cam_resp_ms8101_512\n");
	api1();
	return 1;
}

int toipcam_request_ms8101_513()
{
	printf("toipcam_request_ms8101_513\n");
	api2();
	return 1;
}
int toipcam_ack_ms8101_513()
{
	printf("toipcam_ack_ms8101_513\n");
	return 1;
}
int cam_send_ms8101_513()
{
	printf("cam_send_ms8101_513\n");
	return 1;
}
int cam_resp_ms8101_513()
{
	printf("cam_resp_ms8101_513\n");
	api2();
	return 1;
}