/*
 * carxust-can - leitor SocketCAN mínimo para o CarXust.
 *
 * Uso:
 *   carxust-can can0
 *
 * Imprime uma linha por frame no formato aceito pelo app:
 *   (1712345678.123456) can0 100#1122334455667788
 *
 * Licença: Apache-2.0
 */
#include <errno.h>
#include <linux/can.h>
#include <linux/can/raw.h>
#include <net/if.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <time.h>
#include <unistd.h>

static volatile sig_atomic_t running = 1;

static void on_signal(int signum) {
    (void)signum;
    running = 0;
}

static double monotonic_seconds(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (double)ts.tv_sec + (double)ts.tv_nsec / 1000000000.0;
}

int main(int argc, char **argv) {
    const char *ifname = argc > 1 ? argv[1] : "can0";
    int fd;
    struct sockaddr_can addr;
    struct ifreq ifr;
    struct can_frame frame;

    signal(SIGINT, on_signal);
    signal(SIGTERM, on_signal);

    fd = socket(PF_CAN, SOCK_RAW, CAN_RAW);
    if (fd < 0) {
        perror("socket(PF_CAN)");
        return 1;
    }

    memset(&ifr, 0, sizeof(ifr));
    snprintf(ifr.ifr_name, sizeof(ifr.ifr_name), "%s", ifname);
    if (ioctl(fd, SIOCGIFINDEX, &ifr) < 0) {
        perror("ioctl(SIOCGIFINDEX)");
        close(fd);
        return 1;
    }

    memset(&addr, 0, sizeof(addr));
    addr.can_family = AF_CAN;
    addr.can_ifindex = ifr.ifr_ifindex;

    if (bind(fd, (struct sockaddr *)&addr, sizeof(addr)) < 0) {
        perror("bind(AF_CAN)");
        close(fd);
        return 1;
    }

    fprintf(stderr, "carxust-can: lendo %s\n", ifname);
    fflush(stderr);

    while (running) {
        ssize_t nbytes = read(fd, &frame, sizeof(frame));
        if (nbytes < 0) {
            if (errno == EINTR) continue;
            perror("read(can)");
            break;
        }
        if (nbytes < (ssize_t)sizeof(struct can_frame)) {
            fprintf(stderr, "carxust-can: frame truncado\n");
            continue;
        }

        unsigned int id = frame.can_id & CAN_EFF_MASK;
        int dlc = frame.can_dlc > 8 ? 8 : frame.can_dlc;

        printf("(%.6f) %s %03X#", monotonic_seconds(), ifname, id);
        for (int i = 0; i < dlc; i++) {
            printf("%02X", frame.data[i]);
        }
        printf("\n");
        fflush(stdout);
    }

    close(fd);
    return 0;
}
