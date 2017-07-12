PROG=ncs1024

SRC=socketserver.c


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

socket_server: $(OBJ)
	$(CC) $(CFLAGS) -o $@ $(OBJ) $(LIBS)
	rm -f $(OBJ)

clean:
	rm -f $(OBJ) $(PROG) *.core
