#include "stdlib.h"
#include "stdio.h"

int main(void){
    char* progargv[1] = {"test"};
    exec("exit.coff", 1, progargv);
    exit(0);
}