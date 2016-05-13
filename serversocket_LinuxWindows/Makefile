PROG=socketpack

SRC=socketpack.c


CC    = gcc -D_REENTRANT
OBJ   =	$(SRC:.c=.o)
LIBS  += -lpthread
#INCLUDE_API = -include venderAPI_YY.h
#./libarontekh264.a
#-liconv
CFLAGS+= -I/usr/local/include -L/usr/local/lib -Wall

all: $(PROG)

$(PROG): $(OBJ)
	$(CC) $(CFLAGS) $(INCLUDE_API) -o $@ $(OBJ) $(LIBS)
	rm -f $(OBJ)

clean:
	rm -f $(OBJ) *.d
