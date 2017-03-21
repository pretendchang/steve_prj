PROG=ncs1024

SRC=nc_controller.c
SRC+=plugin1/plugin1.c plugin1/plugin1_command.c
SRC+=plugin2/plugin2.c plugin2/plugin2_command.c


CC    = gcc -D_REENTRANT
OBJ   =	$(SRC:.c=.o)
LIBS  += -lpthread 
#./libarontekh264.a
#-liconv 
CFLAGS+= -I/usr/local/include -L/usr/local/lib -Wall

#all: $(PROG)

#$(PROG): $(OBJ)
#	$(CC) $(CFLAGS) -o $@ $(OBJ) $(LIBS)
#	rm -f $(OBJ)

#install:
#	install -s -m 0755 -o root $(PROG) /sbin/$(PROG)

core: ncs_core

ncs_core: $(OBJ)
	$(CC) $(CFLAGS) -o $@ $(OBJ) protocol.ld $(LIBS)
	rm -f $(OBJ)

nb: ncs_nb

ncs_nb: $(OBJ)
	$(CC) $(CFLAGS) -o $@ $(OBJ) $(LIBS)
	rm -f $(OBJ)

demo: ncs_demo

ncs_demo: $(OBJ)
	$(CC) $(CFLAGS) -o $@ $(OBJ) $(LIBS)
	rm -f $(OBJ)	

bench: ncs_bench

ncs_bench: $(OBJ)
	$(CC) $(CFLAGS) -o $@ $(OBJ) $(LIBS)
	rm -f $(OBJ)

vphone: ncs_vphone

ncs_vphone: $(OBJ)
	$(CC) $(CFLAGS) -o $@ $(OBJ) $(LIBS)
	rm -f $(OBJ)

clean:
	rm -f $(OBJ) $(PROG) *.core
