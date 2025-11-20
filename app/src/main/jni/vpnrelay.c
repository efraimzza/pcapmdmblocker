#include <jni.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <poll.h>
#include <netinet/in.h> // for IPPROTO_TCP, IPPROTO_UDP, in_addr, in6_addr
#include <netinet/ip.h> // For IPv4 header
#include <netinet/ip6.h> // For IPv6 header
#include <netinet/tcp.h>
#include <netinet/udp.h>
#include <arpa/inet.h>  // For inet_ntop, inet_pton
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <string.h>
// #include <netdb.h>      // Removed for DNS proxy; not needed for getaddrinfo directly
#include <stdarg.h>     // For log
#include <sys/socket.h> // For socket options, sockaddr_storage, sa_family_t
#include <sys/types.h>  // For ssize_t, etc.
#include "pcapdroid.h"
#include "common/utils.h"
#include <pthread.h>
#include "log_writer.h"
#include "port_map.h"
#include "ndpi_protocol_ids.h"
#include "ndpi_main.h"
#include "ndpi_api.h"


static int netd_resolve_waiting;
static u_int64_t last_connections_dump;
static u_int64_t next_connections_dump;
bool domainopen=false;
bool thischeck=false;
bool debug=true;
bool running=false;
bool dump_capture_stats_now = false;
bool reload_blacklists_now = false;
int bl_num_checked_connections = 0;
int fw_num_checked_connections = 0;
uint32_t new_dns_server = 0;
bool has_seen_dump_extensions = false;
extern int run_vpn(pcapdroid_t *pd);
//extern int run_pcap(pcapdroid_t *pd);

static ndpi_protocol_bitmask_struct_t masterProtos;
static bool masterProtosInit = false;


// --- קבועים ---
#define LOG_PATH "/storage/emulated/0/log.txt"
#define LOG_DOMAIN_PATH "/storage/emulated/0/logdomain.txt" // קובץ לוג נפרד לדומיינים
#define LOG_PKT_PATH "/storage/emulated/0/logpkt.txt" // קובץ לוג נפרד 
/*
#define MAX_SESSIONS 512
#define SESSION_TIMEOUT 120 // שניות (הוגדל מ-60)

// גודל באפר מרבי לקריאה/כתיבה
#define MAX_PACKET_SIZE 65536 // מקסימום IP packet size (IPv4 or IPv6)

// גודל MSS מומלץ עבור VPN (למניעת פרגמנטציה). בדרך כלל 1400-1420 עבור MTU 1500
#define VPN_RECOMMENDED_MSS 1400

// מצבי TCP Enum
#define TCP_STATE_CLOSED 0
#define TCP_STATE_SYN_SENT 1       // הלקוח שלח SYN, ממתין ל-SYN-ACK
#define TCP_STATE_SYN_RECEIVED 2   // השרת קיבל SYN, שלח SYN-ACK, ממתין ל-ACK
#define TCP_STATE_ESTABLISHED 3    // החיבור פתוח, ניתן להחליף נתונים
#define TCP_STATE_FIN_WAIT_1 4     // הלקוח שלח FIN, ממתין ל-ACK
#define TCP_STATE_FIN_WAIT_2 5     // הלקוח קיבל ACK עבור FIN, ממתין ל-FIN
#define TCP_STATE_CLOSING 6        // שני הצדדים שלחו FIN, ממתינים ל-ACK של ה-FIN של הלקוח
#define TCP_STATE_TIME_WAIT 7      // הלקוח קיבל FIN, שלח ACK, ממתין ל-2MSL
#define TCP_STATE_CLOSE_WAIT 8     // השרת קיבל FIN, שלח ACK, ממתין שהאפליקציה שלו תסגור
#define TCP_STATE_LAST_ACK 9       // השרת שלח FIN, ממתין ל-ACK

// --- מבנים עבור כותרות IP/TCP/UDP ---
// מבנה כותרת IP (IPv4)
struct ip_header {
    unsigned char ip_hl:4, ip_v:4; // אורך כותרת (4 ביט), גרסה (4 ביט)
    unsigned char ip_tos;         // סוג שירות
    unsigned short ip_len;        // אורך כולל (בתים)
    unsigned short ip_id;         // זיהוי
    unsigned short ip_off;        // שדה היסט פרגמנט (כולל דגלים)
    unsigned char ip_ttl;         // זמן חיים
    unsigned char ip_p;           // פרוטוקול
    unsigned short ip_sum;        // Checksum
    uint32_t ip_src, ip_dst;      // כתובות IP מקור ויעד
};

// מבנה כותרת TCP
struct tcp_header {
    unsigned short th_sport; // פורט מקור
    unsigned short th_dport; // פורט יעד
    uint32_t th_seq;         // מספר רצף
    uint32_t th_ack;         // מספר אישור
    unsigned char th_off:4, th_x2:4; // היסט נתונים (4 ביט), שמור (4 ביט)
    unsigned char th_flags;  // דגלים (SYN, ACK, FIN, וכו')
#define TH_FIN  0x01
#define TH_SYN  0x02
#define TH_RST  0x04
#define TH_PUSH 0x08
#define TH_ACK  0x10
#define TH_URG  0x20
    unsigned short th_win;   // גודל חלון
    unsigned short th_sum;   // Checksum
    unsigned short th_urp;   // מצביע דחוף
};

// מבנה כותרת UDP
struct udp_header {
    unsigned short uh_sport; // פורט מקור
    unsigned short uh_dport; // פורט יעד
    unsigned short uh_len;   // אורך UDP
    unsigned short uh_sum;   // Checksum של UDP
};

// מבנה כותרת DNS
struct dns_header {
    uint16_t id;         // מספר זיהוי (Transaction ID)
    uint16_t flags;      // דגלים
    uint16_t q_count;    // מספר שאילתות
    uint16_t ans_count;  // מספר רשומות תשובה
    uint16_t auth_count; // מספר רשומות סמכות
    uint16_t add_count;  // מספר רשומות נוספות
};

// מבנה עבור שאילתת DNS (חלק קבוע)
struct dns_question_section {
    // QNAME (שם) בא מיד לפני QTYPE/QCLASS
    uint16_t qtype;
    uint16_t qclass;
} __attribute__((packed)); // ensure no padding


// מבנה עבור רשומת משאבים (Resource Record) של DNS (חלק קבוע, אחרי השם)
struct dns_record_section {
    // NAME (שם) בא מיד לפני TYPE/CLASS/TTL/RDLENGTH/RDATA
    uint16_t type;
    uint16_t class;
    uint32_t ttl;
    uint16_t rdlength;
    // RDATA (נתונים) בא מיד אחרי rdlength
} __attribute__((packed)); // ensure no padding


// מבנה סשן TCP - מאחסן מצב עבור חיבור TCP
typedef struct {
    struct sockaddr_storage local_addr;  // Client (TUN) address (IPv4 or IPv6)
    socklen_t local_addr_len;
    struct sockaddr_storage remote_addr; // Destination address (IPv4 or IPv6)
    socklen_t remote_addr_len;
    sa_family_t addr_family;             // AF_INET or AF_INET6

    int remote_socket_fd;    // הסוקט המחובר לשרת האינטרנט האמיתי
    time_t last_active;      // חותמת זמן פעילות אחרונה לטיימאאוט
    int state;               // מצב מכונת המצבים של TCP

    // לוגיקת מספרי רצף ואישור ב-TCP
    // client_seq_num: מספר הרצף הבא שה-VPN מצפה לקבל מהלקוח (TUN).
    //                 זה יהיה ה-th_ack שאנו שולחים ללקוח.
    uint32_t client_seq_num;

    // server_seq_num: מספר הרצף שבו ה-VPN ישתמש עבור חבילות הנשלחות ללקוח (TUN).
    //                 זה יהיה ה-th_seq שאנו שולחים ללקוח.
    uint32_t server_seq_num;
    
    // server_ack_num: המספר שבו השרת המרוחק (האמיתי) אישר את הנתונים שלנו.
    //                 משמש לעדכון server_seq_num כאשר השרת המרוחק שולח לנו ACK.
    uint32_t server_ack_num; 

    uint32_t window_size;    // גודל חלון (לשני הצדדים, מפושט)
    unsigned short ip_id_counter; // מונה עבור IP ID (IPv4 only, אך נשמר לפשטות)
    char hostname[256];      // שם הדומיין שזוהה עבור סשן זה (HTTP / SNI)
    uint16_t client_mss;     // MSS שהלקוח פרסם
    uint16_t server_mss;     // MSS שהשרת פרסם
} tcp_session_t;

// --- משתנים גלובליים ---
static tcp_session_t sessions[MAX_SESSIONS];
//static int running = 1;
static int tun_fd_global = -1; // שמירת tun_fd באופן גלובלי עבור פעולות כתיבה

// שרתי DNS חיצוניים (ניתן להגדיר מספר)
// עבור DNS proxy, אלו שרתי ה-DNS שה-VPN ישלח אליהם את השאילתות.
static struct sockaddr_storage dns_servers[2]; // 0 for IPv4, 1 for IPv6
static socklen_t dns_server_lens[2];
static int dns_server_count = 0;

*/
// --- פונקציות עזר ללוג ---

// פונקציה לרישום לוג לקובץ כללי
void log_to_file(const char *format, ...) {
    if(!debug)
        return;
    FILE *fp = fopen(LOG_PATH, "a");
    if (!fp) return;

    time_t now = time(NULL);
    struct tm *t = localtime(&now);
    fprintf(fp, "[%02d:%02d:%02d] ", t->tm_hour, t->tm_min, t->tm_sec);

    va_list args;
    va_start(args, format);
    vfprintf(fp, format, args);
    va_end(args);

    fprintf(fp, "\n");
    fclose(fp);
}

// פונקציה לרישום לוג לקובץ דומיינים ספציפי
void log_domain_to_file(const char *format, ...) {
    FILE *fp = fopen(LOG_DOMAIN_PATH, "a");
    if (!fp) return;

    time_t now = time(NULL);
    struct tm *t = localtime(&now);
    fprintf(fp, "[%02d:%02d:%02d] ", t->tm_hour, t->tm_min, t->tm_sec);

    va_list args;
    va_start(args, format);
    vfprintf(fp, format, args);
    va_end(args);

    fprintf(fp, "\n");
    fclose(fp);
}

void log_pkt_to_file(const char *format) {
    FILE *fp = fopen(LOG_PKT_PATH, "a");
    if (!fp) return;

    time_t now = time(NULL);
    struct tm *t = localtime(&now);
    fprintf(fp, "[%02d:%02d:%02d] ", t->tm_hour, t->tm_min, t->tm_sec);

   
    
    fprintf(fp, "%s",format);
    

    fprintf(fp, "\n");
    fclose(fp);
}
/*
// פונקציה עזר להדפסת כתובת IP (IPv4 או IPv6)
const char* addr_to_string(const struct sockaddr_storage *addr, char *buf, size_t buf_len) {
    if (addr->ss_family == AF_INET) {
        const struct sockaddr_in *s = (const struct sockaddr_in *)addr;
        inet_ntop(AF_INET, &s->sin_addr, buf, buf_len);
    } else if (addr->ss_family == AF_INET6) {
        const struct sockaddr_in6 *s = (const struct sockaddr_in6 *)addr;
        inet_ntop(AF_INET6, &s->sin6_addr, buf, buf_len);
    } else {
        strncpy(buf, "UNKNOWN_AF", buf_len);
    }
    buf[buf_len - 1] = '\0';
    return buf;
}


// --- פונקציות עזר כלליות ---

// חישוב Checksum עבור IP/TCP/UDP
uint16_t checksum(const void *data, int len) {
    const uint16_t *buf = data;
    uint32_t sum = 0;
    while (len > 1) {
        sum += *buf++;
        len -= 2;
    }
    if (len == 1) {
        sum += *(uint8_t *)buf;
    }
    while (sum >> 16) {
        sum = (sum & 0xFFFF) + (sum >> 16);
    }
    return (uint16_t)~sum;
}

// חישוב Checksum של TCP/UDP עם Pseudo-Header (IPv4)
uint16_t transport_checksum_ipv4(uint32_t src_ip, uint32_t dst_ip, uint8_t protocol,
                               const void *transport_header, int transport_header_len,
                               const void *payload, int payload_len) {
    struct {
        uint32_t src_ip;
        uint32_t dst_ip;
        uint8_t zero;
        uint8_t protocol;
        uint16_t len;
    } pseudo_header;

    pseudo_header.src_ip = src_ip;
    pseudo_header.dst_ip = dst_ip;
    pseudo_header.zero = 0;
    pseudo_header.protocol = protocol;
    pseudo_header.len = htons(transport_header_len + payload_len);

    int total_len = sizeof(pseudo_header) + transport_header_len + payload_len;
    uint8_t *buf = malloc(total_len + (total_len % 2));
    if (!buf) {
        log_to_file("הקצאת זיכרון נכשלה עבור transport checksum (IPv4)");
        return 0;
    }

    memcpy(buf, &pseudo_header, sizeof(pseudo_header));
    memcpy(buf + sizeof(pseudo_header), transport_header, transport_header_len);
    if (payload && payload_len > 0) {
        memcpy(buf + sizeof(pseudo_header) + transport_header_len, payload, payload_len);
    }
    // ריפוד ב-0 אם האורך הכולל אי זוגי
    if (total_len % 2 != 0) {
        buf[total_len] = 0;
        total_len++;
    }

    uint16_t result = checksum(buf, total_len);
    free(buf);
    return result;
}

// חישוב Checksum של TCP/UDP עם Pseudo-Header (IPv6)
uint16_t transport_checksum_ipv6(const struct in6_addr *src_ip6, const struct in6_addr *dst_ip6,
                                 uint8_t protocol, uint16_t transport_len,
                                 const void *transport_header, int transport_header_len,
                                 const void *payload, int payload_len) {
    struct {
        struct in6_addr src_addr;
        struct in6_addr dst_addr;
        uint32_t upper_layer_len; // In network byte order
        uint8_t zero[3];
        uint8_t next_header;      // Protocol
    } pseudo_header;

    memset(&pseudo_header, 0, sizeof(pseudo_header));
    pseudo_header.src_addr = *src_ip6;
    pseudo_header.dst_addr = *dst_ip6;
    pseudo_header.upper_layer_len = htonl(transport_len); // TCP/UDP header + payload length
    pseudo_header.next_header = protocol;

    int total_len = sizeof(pseudo_header) + transport_header_len + payload_len;
    uint8_t *buf = malloc(total_len + (total_len % 2));
    if (!buf) {
        log_to_file("הקצאת זיכרון נכשלה עבור transport checksum (IPv6).");
        return 0;
    }

    memcpy(buf, &pseudo_header, sizeof(pseudo_header));
    memcpy(buf + sizeof(pseudo_header), transport_header, transport_header_len);
    if (payload && payload_len > 0) {
        memcpy(buf + sizeof(pseudo_header) + transport_header_len, payload, payload_len);
    }
    if (total_len % 2 != 0) {
        buf[total_len] = 0; // Pad with zero if odd length
        total_len++;
    }

    uint16_t result = checksum(buf, total_len);
    free(buf);
    return result;
}


// פונקציה לחילוץ שם דומיין ממטען HTTP (מצפה לכותרת "Host:")
const char* extract_hostname_from_http_payload(const char *payload, int payload_len) {
    static char hostname_buffer[256];
    const char *host_str = "Host: ";
    const char *start_search = payload;
    const char *end_payload = payload + payload_len;

    while (start_search < end_payload && (start_search = (const char*)memmem(start_search, end_payload - start_search, host_str, strlen(host_str))) != NULL) {
        start_search += strlen(host_str);
        const char *end_line = (const char*)memchr(start_search, '\r', end_payload - start_search);
        if (!end_line) {
            end_line = (const char*)memchr(start_search, '\n', end_payload - start_search);
        }

        if (end_line && (end_line - start_search < sizeof(hostname_buffer) - 1)) {
            size_t len = end_line - start_search;
            strncpy(hostname_buffer, start_search, len);
            hostname_buffer[len] = '\0';
            return hostname_buffer;
        }
        start_search = end_line ? end_line + 1 : end_payload;
    }
    return NULL;
}

// פונקציה לחילוץ SNI (Server Name Indication) מהודעת TLS ClientHello
const char* extract_sni_hostname_from_tls_client_hello(const uint8_t *payload, int payload_len) {
    static char sni_hostname_buffer[256];
    sni_hostname_buffer[0] = '\0';

    if (payload_len < 5) { // Block start for payload_len < 5
        return NULL;
    } // Block end for payload_len < 5

    // Check TLS Record Type (Handshake 0x16) and TLS Version (0x0301-0x0304 for TLS 1.0-1.3)
    if (payload[0] != 0x16 || (payload[1] != 0x03 || (payload[2] < 0x01 || payload[2] > 0x04))) { // Block start for TLS Record check
        return NULL;
    } // Block end for TLS Record check

    uint16_t record_length = ntohs(*(uint16_t*)(payload + 3));
    if (record_length > (payload_len - 5)) { // Block start for record_length check
        return NULL;
    } // Block end for record_length check

    const uint8_t *handshake_start = payload + 5;
    int handshake_len = record_length;

    if (handshake_len < 4) { // Block start for handshake_len check
        return NULL; // Min Handshake Header (Type + Length)
    } // Block end for handshake_len check

    // Check Handshake Type (ClientHello 0x01)
    if (handshake_start[0] != 0x01) { // Block start for Handshake Type check
        return NULL;
    } // Block end for Handshake Type check

    // ClientHello message length
    uint32_t client_hello_msg_len = (handshake_start[1] << 16) | (handshake_start[2] << 8) | handshake_start[3];
    if (handshake_len < (4 + client_hello_msg_len)) { // Block start for client_hello_msg_len check
        return NULL;
    } // Block end for client_hello_msg_len check

    const uint8_t *ptr = handshake_start + 4; // After Handshake header

    // Skip ClientVersion (2 bytes)
    ptr += 2;
    // Skip Random (32 bytes)
    ptr += 32;

    // Skip Session ID (1 byte length + length bytes)
    if ((ptr - payload) >= payload_len) { // Block start for Session ID check
        return NULL;
    } // Block end for Session ID check
    uint8_t session_id_len = *ptr++;
    if ((ptr - payload) + session_id_len > payload_len) { // Block start for Session ID length check
        return NULL;
    } // Block end for Session ID length check
    ptr += session_id_len;

    // Skip Cipher Suites (2 byte length + length bytes)
    if ((ptr - payload) >= (payload_len - 1)) { // Block start for Cipher Suites check
        return NULL;
    } // Block end for Cipher Suites check
    uint16_t cipher_suites_len = ntohs(*(uint16_t*)ptr);
    ptr += 2;
    if ((ptr - payload) + cipher_suites_len > payload_len) { // Block start for Cipher Suites length check
        return NULL;
    } // Block end for Cipher Suites length check
    ptr += cipher_suites_len;

    // Skip Compression Methods (1 byte length + length bytes)
    if ((ptr - payload) >= payload_len) { // Block start for Compression Methods check
        return NULL;
    } // Block end for Compression Methods check
    uint8_t comp_methods_len = *ptr++;
    if ((ptr - payload) + comp_methods_len > payload_len) { // Block start for Compression Methods length check
        return NULL;
    } // Block end for Compression Methods length check
    ptr += comp_methods_len;

    // Extensions Length
    if ((ptr - payload) >= (payload_len - 1)) { // Block start for Extensions Length check
        return NULL;
    } // Block end for Extensions Length check
    uint16_t extensions_len = ntohs(*(uint16_t*)ptr);
    ptr += 2;

    if ((ptr - payload) + extensions_len > payload_len) { // Block start for Extensions Length check 2
        return NULL;
    } // Block end for Extensions Length check 2

    const uint8_t *extensions_end = ptr + extensions_len;

    // Loop through extensions to find SNI (Type 0x0000)
    while (ptr < extensions_end) { // Loop start for extensions
        if ((ptr + 4) > extensions_end) { // Block start for Extension header check
            break; // Not enough bytes for Extension header
        } // Block end for Extension header check
        uint16_t extension_type = ntohs(*(uint16_t*)ptr);
        ptr += 2;
        uint16_t extension_data_len = ntohs(*(uint16_t*)ptr);
        ptr += 2;

        if ((ptr + extension_data_len) > extensions_end) { // Block start for Extension data length check
            break; // Extension data too long
        } // Block end for Extension data length check

        if (extension_type == 0x0000) { // SNI extension (Block start)
            if ((ptr + 2) > extensions_end) { // Block start for SNI list length check
                break;
            } // Block end for SNI list length check
            uint16_t server_name_list_len = ntohs(*(uint16_t*)ptr);
            ptr += 2;

            if ((ptr + server_name_list_len) > extensions_end) { // Block start for Server Name List length check
                break;
            } // Block end for Server Name List length check

            const uint8_t *name_list_end = ptr + server_name_list_len;

            while (ptr < name_list_end) { // Loop start for server names
                if ((ptr + 3) > name_list_end) { // Block start for Name Type/Length check
                    break; // Type (1 byte) + Length (2 bytes)
                } // Block end for Name Type/Length check
                uint8_t name_type = *ptr++; // 0x00 for HostName
                uint16_t name_len = ntohs(*(uint16_t*)ptr);
                ptr += 2;

                if (name_len >= sizeof(sni_hostname_buffer) || (ptr + name_len) > name_list_end) { // Block start for buffer overflow check
                     log_to_file("SNI hostname too long or out of bounds. Truncating.");
                     name_len = (sizeof(sni_hostname_buffer) - 1 < name_len) ? (sizeof(sni_hostname_buffer) - 1) : name_len;
                     // Continue to copy what's safe, and ensure null termination
                } // Block end for buffer overflow check


                if (name_type == 0x00) { // HostName (Block start)
                    strncpy(sni_hostname_buffer, (const char*)ptr, name_len);
                    sni_hostname_buffer[name_len] = '\0';
                    return sni_hostname_buffer;
                } // HostName (Block end)
                ptr += name_len;
            } // Loop end for server names
        } // SNI extension (Block end)
        ptr += extension_data_len;
    } // Loop end for extensions
    return NULL;
} // End of extract_sni_hostname_from_tls_client_hello


// חיפוש סשן TCP קיים על בסיס החמישייה (5-tuple)
static tcp_session_t* find_session(const struct sockaddr_storage *local_addr, const struct sockaddr_storage *remote_addr) {
    char local_ip_str[INET6_ADDRSTRLEN], remote_ip_str[INET6_ADDRSTRLEN];
    uint16_t local_port, remote_port;

    if (local_addr->ss_family == AF_INET) { // Block start for AF_INET
        const struct sockaddr_in *s_local = (const struct sockaddr_in *)local_addr;
        const struct sockaddr_in *s_remote = (const struct sockaddr_in *)remote_addr;
        local_port = s_local->sin_port; // Correct for AF_INET
        remote_port = s_remote->sin_port; // Correct for AF_INET
    } else if (local_addr->ss_family == AF_INET6) { // Block start for AF_INET6
        const struct sockaddr_in6 *s_local = (const struct sockaddr_in6 *)local_addr;
        const struct sockaddr_in6 *s_remote = (const struct sockaddr_in6 *)remote_addr;
        local_port = s_local->sin6_port; 
        remote_port = s_remote->sin6_port; 
    } else { // Block start for unsupported AF
        return NULL; // Unsupported address family
    } // Block end for address family determination

    for (int i = 0; i < MAX_SESSIONS; i++) { // Loop start for sessions
        if (sessions[i].remote_socket_fd != -1 && sessions[i].addr_family == local_addr->ss_family) { // Session active and family matches
            // Compare addresses and ports based on family
            if (sessions[i].addr_family == AF_INET) { // Block start for AF_INET comparison
                const struct sockaddr_in *sess_local = (const struct sockaddr_in *)&sessions[i].local_addr;
                const struct sockaddr_in *sess_remote = (const struct sockaddr_in *)&sessions[i].remote_addr;
                if (sess_local->sin_port == local_port && sess_remote->sin_port == remote_port &&
                    sess_local->sin_addr.s_addr == ((const struct sockaddr_in *)local_addr)->sin_addr.s_addr &&
                    sess_remote->sin_addr.s_addr == ((const struct sockaddr_in *)remote_addr)->sin_addr.s_addr) { // All match
                    return &sessions[i];
                }
            } else if (sessions[i].addr_family == AF_INET6) { // Block start for AF_INET6 comparison
                const struct sockaddr_in6 *sess_local = (const struct sockaddr_in6 *)&sessions[i].local_addr;
                const struct sockaddr_in6 *sess_remote = (const struct sockaddr_in6 *)&sessions[i].remote_addr;
                if (sess_local->sin6_port == local_port && sess_remote->sin6_port == remote_port && 
                    memcmp(&sess_local->sin6_addr, &((const struct sockaddr_in6 *)local_addr)->sin6_addr, sizeof(struct in6_addr)) == 0 &&
                    memcmp(&sess_remote->sin6_addr, &((const struct sockaddr_in6 *)remote_addr)->sin6_addr, sizeof(struct in6_addr)) == 0) {
                    return &sessions[i];
                }
            }
        }
    } // Loop end for sessions
    return NULL;
} // End of find_session

// יצירת סשן TCP חדש
static tcp_session_t* create_session(const struct sockaddr_storage *local_addr, socklen_t local_addr_len,
                                     const struct sockaddr_storage *remote_addr, socklen_t remote_addr_len,
                                     sa_family_t addr_family) { // Function start
    char local_ip_str[INET6_ADDRSTRLEN], remote_ip_str[INET6_ADDRSTRLEN];
    uint16_t local_port, remote_port;

    if (addr_family == AF_INET) { // Block start for AF_INET
        local_port = ((const struct sockaddr_in *)local_addr)->sin_port;
        remote_port = ((const struct sockaddr_in *)remote_addr)->sin_port;
    } else { // AF_INET6
        const struct sockaddr_in6 *temp_local_addr_in6 = (const struct sockaddr_in6 *)local_addr;
        const struct sockaddr_in6 *temp_remote_addr_in6 = (const struct sockaddr_in6 *)remote_addr;
        local_port = temp_local_addr_in6->sin6_port;
        remote_port = temp_remote_addr_in6->sin6_port;
    } // Block end for address family port extraction

    for (int i = 0; i < MAX_SESSIONS; i++) { // Loop start for sessions
        if (sessions[i].remote_socket_fd == -1) { // מציאת מקום פנוי (Block start)
            memset(&sessions[i], 0, sizeof(tcp_session_t)); // איפוס המבנה
            memcpy(&sessions[i].local_addr, local_addr, local_addr_len);
            sessions[i].local_addr_len = local_addr_len;
            memcpy(&sessions[i].remote_addr, remote_addr, remote_addr_len);
            sessions[i].remote_addr_len = remote_addr_len;
            sessions[i].addr_family = addr_family;

            sessions[i].last_active = time(NULL);
            sessions[i].state = TCP_STATE_CLOSED; 

            sessions[i].client_seq_num = 0; // יאותחל כשנקבל SYN מהלקוח
            sessions[i].server_seq_num = rand(); // מספר רצף התחלתי עבור SYN-ACK מהשרת
            sessions[i].server_ack_num = 0; // יאותחל כשנקבל ACK משרת היעד

            sessions[i].window_size = htons(65535); // גודל חלון מקסימלי (לוגי)
            sessions[i].remote_socket_fd = -1; // אתחול ל-1-, ייקבע בעת התחברות
            sessions[i].ip_id_counter = 0; // אתחול מונה IP ID
            sessions[i].hostname[0] = '\0'; // אתחול שם דומיין ריק
            sessions[i].client_mss = 0;
            sessions[i].server_mss = 0;

            log_to_file("סשן TCP חדש נוצר: %s:%d -> %s:%d (FD: -1, AF: %d)",
                        addr_to_string(&sessions[i].local_addr, local_ip_str, sizeof(local_ip_str)), ntohs(local_port),
                        addr_to_string(&sessions[i].remote_addr, remote_ip_str, sizeof(remote_ip_str)), ntohs(remote_port),
                        addr_family);
            return &sessions[i];
        } // מציאת מקום פנוי (Block end)
    } // Loop end for sessions
    log_to_file("אין חריצי סשן TCP פנויים עבור סשן חדש.");
    return NULL;
} // End of create_session

// ניקוי סשנים לא פעילים
static void cleanup_sessions() { // Function start
    time_t now = time(NULL);
    char local_ip_str[INET6_ADDRSTRLEN], remote_ip_str[INET6_ADDRSTRLEN];
    for (int i = 0; i < MAX_SESSIONS; i++) { // Loop start for sessions
        if (sessions[i].remote_socket_fd != -1) { // Session is active
            double diff = difftime(now, sessions[i].last_active);
            if (diff > SESSION_TIMEOUT) { // Timeout occurred
                uint16_t local_port, remote_port;
                if (sessions[i].addr_family == AF_INET) { // Block start for AF_INET
                    local_port = ((const struct sockaddr_in *)&sessions[i].local_addr)->sin_port;
                    remote_port = ((const struct sockaddr_in *)&sessions[i].remote_addr)->sin_port;
                } else { // AF_INET6
                    const struct sockaddr_in6 *temp_local_in6_ptr = (const struct sockaddr_in6 *)&sessions[i].local_addr;
                    const struct sockaddr_in6 *temp_remote_in6_ptr = (const struct sockaddr_in6 *)&sessions[i].remote_addr;
                    local_port = temp_local_in6_ptr->sin6_port;
                    remote_port = temp_remote_in6_ptr->sin6_port;
                } // Block end for port extraction

                log_to_file("סשן חרג מזמן ונסגר: %s:%d -> %s:%d (דומיין: %s, מצב: %d)",
                            addr_to_string(&sessions[i].local_addr, local_ip_str, sizeof(local_ip_str)), ntohs(local_port),
                            addr_to_string(&sessions[i].remote_addr, remote_ip_str, sizeof(remote_ip_str)), ntohs(remote_port),
                            sessions[i].hostname[0] != '\0' ? sessions[i].hostname : "לא ידוע",
                            sessions[i].state);
                log_domain_to_file("סשן חרג מזמן ונסגר: %s:%d -> %s:%d (דומיין: %s)",
                                   addr_to_string(&sessions[i].local_addr, local_ip_str, sizeof(local_ip_str)), ntohs(local_port),
                                   addr_to_string(&sessions[i].remote_addr, remote_ip_str, sizeof(remote_ip_str)), ntohs(remote_port),
                                   sessions[i].hostname[0] != '\0' ? sessions[i].hostname : "לא ידוע");

                close(sessions[i].remote_socket_fd);
                sessions[i].remote_socket_fd = -1; // סימון כפנוי
                sessions[i].state = TCP_STATE_CLOSED; // איפוס מצב
            } // Timeout occurred (Block end)
        } // Session is active (Block end)
    } // Loop end for sessions
} // End of cleanup_sessions

// יצירת סוקט אינטרנט יוצא וחיבור
static int create_and_connect_internet_socket(const struct sockaddr_storage *dst_addr, socklen_t dst_addr_len, sa_family_t addr_family) { // Function start
    int sock = socket(addr_family, SOCK_STREAM, 0); // SOCK_STREAM עבור TCP
    if (sock < 0) { // Socket creation failed
        log_to_file("יצירת סוקט נכשלה (AF: %d): %s", addr_family, strerror(errno));
        return -1;
    } // End of socket creation failed

    // הגדרת סוקט כלא חוסם
    int flags = fcntl(sock, F_GETFL, 0);
    if (flags < 0) { // Get flags failed
        log_to_file("שגיאה בקבלת דגלי סוקט: %s", strerror(errno));
        close(sock);
        return -1;
    } // End of get flags failed
    if (fcntl(sock, F_SETFL, flags | O_NONBLOCK) < 0) { // Set non-blocking failed
        log_to_file("שגיאה בהגדרת סוקט כלא חוסם: %s", strerror(errno));
        close(sock);
        return -1;
    } // End of set non-blocking failed

    char dst_ip_str[INET6_ADDRSTRLEN];
    uint16_t dst_port;
    if (addr_family == AF_INET) { // Block start for AF_INET
        dst_port = ((const struct sockaddr_in *)dst_addr)->sin_port;
    } else { // AF_INET6
        const struct sockaddr_in6 *temp_dst_addr_in6 = (const struct sockaddr_in6 *)dst_addr;
        dst_port = temp_dst_addr_in6->sin6_port;
    } // Block end for port extraction

    log_to_file("מנסה להתחבר אל: %s:%d (FD: %d, AF: %d)",
                addr_to_string(dst_addr, dst_ip_str, sizeof(dst_ip_str)), ntohs(dst_port), sock, addr_family);
    int connect_ret = connect(sock, (const struct sockaddr *)dst_addr, dst_addr_len);
    if (connect_ret < 0 && errno != EINPROGRESS) { // Connect failed immediately
        log_to_file("שגיאת חיבור לשרת באופן מיידי: %s", strerror(errno));
        close(sock);
        return -1;
    } // End of connect failed immediately

    return sock;
} // End of create_and_connect_internet_socket

// פונקציית עזר לבנייה ושליחת חבילה חזרה להתקן TUN
void send_packet_to_tun(const struct sockaddr_storage *src_addr, const struct sockaddr_storage *dst_addr,
                        uint8_t protocol, const void *transport_header, int transport_header_len,
                        const void *payload, int payload_len, unsigned short ip_id) { // Function start
    
    // בדיקה אם ה-TUN file descriptor עדיין תקין
    if (tun_fd_global < 0) { // Block start for tun_fd_global check
        log_to_file("שגיאה בכתיבה ל-TUN: TUN FD אינו תקין (%d).", tun_fd_global);
        return;
    } // Block end for tun_fd_global check

    char *packet_buf;
    int ip_total_len;

    if (src_addr->ss_family == AF_INET) { // Block start for AF_INET
        ip_total_len = sizeof(struct ip_header) + transport_header_len + payload_len;
        packet_buf = (char*)malloc(ip_total_len);
        if (!packet_buf) { // Malloc failed
            log_to_file("הקצאת באפר נכשלה עבור חבילה ל-TUN (IPv4).");
            return;
        } // End of malloc failed

        struct ip_header *iph = (struct ip_header*)packet_buf;
        memset(iph, 0, sizeof(struct ip_header));
        iph->ip_hl = 5;
        iph->ip_v = 4;
        iph->ip_tos = 0;
        iph->ip_len = htons(ip_total_len);
        iph->ip_id = htons(ip_id);
        iph->ip_off = htons(0x4000); // Don't Fragment
        iph->ip_ttl = 64;
        iph->ip_p = protocol;
        iph->ip_src = ((const struct sockaddr_in *)src_addr)->sin_addr.s_addr;
        iph->ip_dst = ((const struct sockaddr_in *)dst_addr)->sin_addr.s_addr;

        iph->ip_sum = 0;
        iph->ip_sum = checksum(iph, sizeof(struct ip_header));

        memcpy(packet_buf + sizeof(struct ip_header), transport_header, transport_header_len);
        if (payload && payload_len > 0) { // Payload exists
            memcpy(packet_buf + sizeof(struct ip_header) + transport_header_len, payload, payload_len);
        } // End of payload exists
    } else if (src_addr->ss_family == AF_INET6) { // Block start for AF_INET6
        ip_total_len = sizeof(struct ip6_hdr) + transport_header_len + payload_len;
        packet_buf = (char*)malloc(ip_total_len);
        if (!packet_buf) { // Malloc failed
            log_to_file("הקצאת באפר נכשלה עבור חבילה ל-TUN (IPv6).");
            return;
        } // End of malloc failed

        struct ip6_hdr *ip6h = (struct ip6_hdr*)packet_buf;
        memset(ip6h, 0, sizeof(struct ip6_hdr));
        ip6h->ip6_flow = htonl((6 << 28) | (0 << 20) | 0); // Version 6, Traffic Class 0, Flow Label 0
        ip6h->ip6_plen = htons(transport_header_len + payload_len);
        ip6h->ip6_nxt = protocol;
        ip6h->ip6_hlim = 64;
        ip6h->ip6_src = ((const struct sockaddr_in6 *)src_addr)->sin6_addr;
        ip6h->ip6_dst = ((const struct sockaddr_in6 *)dst_addr)->sin6_addr;
        // IPv6 has no header checksum

        memcpy(packet_buf + sizeof(struct ip6_hdr), transport_header, transport_header_len);
        if (payload && payload_len > 0) { // Payload exists
            memcpy(packet_buf + sizeof(struct ip6_hdr) + transport_header_len, payload, payload_len);
        } // End of payload exists

        // For TCP/UDP over IPv6, the checksum includes a pseudo-header
        if (protocol == IPPROTO_TCP) { // Block start for IPPROTO_TCP
            struct tcp_header *tcph = (struct tcp_header*)(packet_buf + sizeof(struct ip6_hdr));
            tcph->th_sum = 0; // Reset before calculating
            
            // Create local copies to ensure correct type is passed to checksum function
            // This is a workaround for very old/strict compilers
            struct in6_addr temp_src_ip6_copy = ip6h->ip6_src;
            struct in6_addr temp_dst_ip6_copy = ip6h->ip6_dst;

            tcph->th_sum = transport_checksum_ipv6(&temp_src_ip6_copy, &temp_dst_ip6_copy, protocol,
                                                    transport_header_len + payload_len,
                                                    tcph, transport_header_len, payload, payload_len);
        } else if (protocol == IPPROTO_UDP) { // Block start for IPPROTO_UDP
            struct udp_header *udph = (struct udp_header*)(packet_buf + sizeof(struct ip6_hdr));
            udph->uh_sum = 0; // Reset before calculating

            // Create local copies to ensure correct type is passed to checksum function
            // This is a workaround for very old/strict compilers
            struct in6_addr temp_src_ip6_copy = ip6h->ip6_src;
            struct in6_addr temp_dst_ip6_copy = ip6h->ip6_dst;

            udph->uh_sum = transport_checksum_ipv6(&temp_src_ip6_copy, &temp_dst_ip6_copy, protocol,
                                                    transport_header_len + payload_len,
                                                    udph, transport_header_len, payload, payload_len);
            if (udph->uh_sum == 0) { // Checksum 0 for UDP IPv6
                udph->uh_sum = 0xFFFF; // UDP checksum 0 means checksum not used, but RFC requires 0xFFFF for IPv6
            }
        } // Block end for IPPROTO_UDP

    } else { // Block start for unsupported address family
        log_to_file("שגיאה: משפחת כתובות לא נתמכת בשליחה ל-TUN (AF: %d).", src_addr->ss_family);
        return;
    } // Block end for unsupported address family

    ssize_t written = write(tun_fd_global, packet_buf, ip_total_len);
    if (written < 0) { // Write failed
        log_to_file("שגיאה בכתיבת חבילה ל-TUN: %s", strerror(errno));
    } else { // Write successful
        // log_to_file("נשלחו %zd בתים להתקן TUN (פרוטוקול %d).", written, protocol); // Avoid excessive logging
    } // End of write result
    free(packet_buf);
} // End of send_packet_to_tun

// פונקציה לחילוץ MSS מאפשרויות TCP
uint16_t extract_mss(const struct tcp_header *tcph) { // Function start
    uint16_t mss = 0;
    // אורך כותרת TCP מינימלי הוא 20 בתים (th_off = 5)
    // אם th_off גדול מ-5, יש אפשרויות TCP
    if ((tcph->th_off * 4) > sizeof(struct tcp_header)) { // Options exist
        const uint8_t *options_ptr = (const uint8_t*)tcph + sizeof(struct tcp_header);
        int options_len = (tcph->th_off * 4) - sizeof(struct tcp_header);
        
        int i = 0;
        while (i < options_len) { // Loop through options
            uint8_t kind = options_ptr[i];
            if (kind == 0) { // End of options list
                break; 
            }
            if (kind == 1) { // NOP
                i++;
                continue;
            }
            if ((i + 1) >= options_len) { // Not enough bytes for length field
                break; 
            }
            uint8_t len = options_ptr[i+1];
            if (len == 0 || (i + len) > options_len) { // Invalid length or goes beyond options_len
                break; 
            }

            if (kind == 2) { // MSS option (Kind 2, Length 4)
                if (len == 4) { // Length matches
                    mss = ntohs(*(uint16_t*)(options_ptr + i + 2));
                    break;
                }
            }
            i += len;
        } // Loop end for options
    } // Options exist (Block end)
    return mss;
} // End of extract_mss

// פונונקציה ליצירת אפשרויות TCP עם MSS
int create_mss_option(uint8_t *options_buf, uint16_t mss_value) { // Function start
    if (!options_buf) { // Null buffer
        return 0;
    } // End of null buffer
    options_buf[0] = 2; // Kind: MSS
    options_buf[1] = 4; // Length: 4 bytes
    *(uint16_t*)(options_buf + 2) = htons(mss_value);
    return 4; // Total length of MSS option
} // End of create_mss_option


// --- לוגיקת טיפול בחבילות ---

// פונקציה לניתוח שם דומיין בשאילתת DNS
const uint8_t* parse_dns_qname(const uint8_t *packet_start, const uint8_t *qname_ptr, char *qname_out, int max_len, const uint8_t *packet_end_ptr) { // Function start
    const uint8_t *current_ptr = qname_ptr;
    int offset = 0;
    qname_out[0] = '\0'; 

    while (current_ptr < packet_end_ptr && *current_ptr != 0 && offset < max_len -1) { // Loop for QNAME parsing
        uint8_t label_len = *current_ptr;
        
        if ((label_len & 0xC0) == 0xC0) { // Pointer (compression) (Block start)
            if ((current_ptr + 2) > packet_end_ptr) { // Out of bounds for pointer
                log_to_file("DNS: Pointer read out of bounds.");
                return NULL;
            } // End of out of bounds for pointer

            uint16_t pointer_offset = ntohs(*(uint16_t*)current_ptr) & 0x3FFF;
            // Ensure pointer doesn't point past the end of the packet, or back into an area already processed
            // (the second check (packet_start + pointer_offset) >= qname_ptr is a heuristic for avoiding self-referential or forward-referencing pointers within the current QNAME segment)
            if ((packet_start + pointer_offset) >= packet_end_ptr || (packet_start + pointer_offset) >= qname_ptr) {
                 log_to_file("DNS: Invalid compression pointer target or loop detected.");
                 return NULL;
            }

            // Recursive call for compressed name
            const uint8_t *next_ptr_after_qname_segment = parse_dns_qname(packet_start, packet_start + pointer_offset, qname_out + offset, max_len - offset, packet_end_ptr);
            if (next_ptr_after_qname_segment == NULL) { // Recursive call failed
                return NULL;
            } // End of recursive call failed
            
            current_ptr += 2; 
            return current_ptr; // Return after handling pointer
        } else { // Label (Block start)
            if ((current_ptr + 1 + label_len) > packet_end_ptr) { // Label length exceeds packet bounds
                log_to_file("DNS: Label length exceeds packet bounds.");
                return NULL;
            } // End of label length exceeds packet bounds

            if (offset > 0 && offset < max_len) { // Add dot if not first label
                qname_out[offset++] = '.'; 
            }

            if ((offset + label_len) >= max_len) { // Check if label will overflow output buffer
                label_len = max_len - 1 - offset; 
                if (label_len <= 0) { // No space left
                    break; 
                }
            }
            
            memcpy(qname_out + offset, current_ptr + 1, label_len);
            offset += label_len;
            qname_out[offset] = '\0'; 
            current_ptr += (label_len + 1); 
        } // Label (Block end)
    } // Loop end for QNAME parsing
    qname_out[offset] = '\0';
    if (current_ptr < packet_end_ptr && *current_ptr == 0) { // Check for null terminator
      return current_ptr + 1; 
    }
    return NULL; 
} // End of parse_dns_qname


// פונקציה לטיפול בשאילתות DNS (פרוקסי DNS)
void handle_dns_query(const struct sockaddr_storage *src_addr, const struct sockaddr_storage *dst_addr,
                      const uint8_t *payload, int payload_len) { // Function start
    if (payload_len < sizeof(struct dns_header)) { // Packet too short
        log_to_file("DNS: Packet too short for header (payload_len: %d).", payload_len);
        return;
    } // End of packet too short

    struct dns_header *dns_hdr_in = (struct dns_header*)payload;
    uint16_t id = ntohs(dns_hdr_in->id);
    uint16_t flags = ntohs(dns_hdr_in->flags);
    uint16_t q_count = ntohs(dns_hdr_in->q_count);

    if ((flags & 0x8000) != 0) { // If it's a response, not a query, ignore
        log_to_file("DNS: Not a query (QR set). Ignoring DNS response from TUN.");
        return;
    } // End of not a query

    if (q_count == 0) { // No questions
        log_to_file("DNS: Query with no questions. Ignoring.");
        return;
    } // End of no questions

    const uint8_t *current_ptr = payload + sizeof(struct dns_header);
    char qname_str[256];
    uint16_t qtype, qclass;
    const uint8_t *payload_end_ptr = payload + payload_len;

    // Parse QNAME and QTYPE/QCLASS for logging
    if (q_count > 0) { // Has questions
        const uint8_t *qname_start_ptr = current_ptr; 
        const uint8_t *next_ptr_after_qname = parse_dns_qname(payload, qname_start_ptr, qname_str, sizeof(qname_str), payload_end_ptr);

        if (!next_ptr_after_qname || (next_ptr_after_qname + sizeof(struct dns_question_section)) > payload_end_ptr) { // Failed to parse QNAME
            log_to_file("DNS: Failed to parse QNAME or question data missing.");
            return;
        } // End of failed to parse QNAME

        qtype = ntohs(*(uint16_t*)next_ptr_after_qname);
        qclass = ntohs(*(uint16_t*)(next_ptr_after_qname + 2));

        log_to_file("DNS Query: ID=%u, Question: %s (Type: %u, Class: %u)", id, qname_str, qtype, qclass);
        log_domain_to_file("DNS Query: %s (Type: %u)", qname_str, qtype);
    } else { // No questions
        log_to_file("DNS Query: ID=%u, No questions found.", id);
    } // End of question parsing

    // --- DNS Proxy Logic ---
    int dns_sock = -1;
    struct sockaddr_storage upstream_dns_addr;
    socklen_t upstream_dns_addr_len = 0;
    
    // Choose the appropriate upstream DNS server based on query's address family preference (A for IPv4, AAAA for IPv6)
    // Or, if we have both, prefer the one matching the query type.
    if (qtype == 28) { // AAAA query, prefer IPv6 DNS server
        if (dns_server_count > 1 && dns_servers[1].ss_family == AF_INET6) { // Has IPv6 DNS server at index 1
            upstream_dns_addr = dns_servers[1];
            upstream_dns_addr_len = dns_server_lens[1];
        } else if (dns_server_count > 0 && dns_servers[0].ss_family == AF_INET6) { // Fallback to first if it's IPv6
            upstream_dns_addr = dns_servers[0];
            upstream_dns_addr_len = dns_server_lens[0];
        } else { // No IPv6 DNS server
            log_to_file("DNS: No IPv6 DNS server configured, cannot forward AAAA query.");
            return;
        }
    } else { // A query or other, prefer IPv4 DNS server
        if (dns_server_count > 0 && dns_servers[0].ss_family == AF_INET) { // Has IPv4 DNS server at index 0
            upstream_dns_addr = dns_servers[0];
            upstream_dns_addr_len = dns_server_lens[0];
        } else if (dns_server_count > 1 && dns_servers[1].ss_family == AF_INET) { // Fallback to second if it's IPv4
             upstream_dns_addr = dns_servers[1];
             upstream_dns_addr_len = dns_server_lens[1];
        } else { // No IPv4 DNS server
            log_to_file("DNS: No IPv4 DNS server configured, cannot forward A query.");
            return;
        }
    } // End of DNS server selection

    // If we couldn't find a suitable DNS server, log and return.
    if (upstream_dns_addr_len == 0) { // No suitable DNS server
        log_to_file("DNS: No suitable upstream DNS server found for forwarding query.");
        return;
    } // End of no suitable DNS server

    dns_sock = socket(upstream_dns_addr.ss_family, SOCK_DGRAM, 0);
    if (dns_sock < 0) { // Socket creation failed
        log_to_file("DNS: Failed to create UDP socket for upstream DNS: %s", strerror(errno));
        return;
    } // End of socket creation failed
    
    // Set socket to non-blocking
    int flags_sock = fcntl(dns_sock, F_GETFL, 0);
    if (flags_sock < 0) { // Get flags failed
        log_to_file("DNS: Error getting socket flags: %s", strerror(errno));
        close(dns_sock);
        return;
    } // End of get flags failed
    if (fcntl(dns_sock, F_SETFL, flags_sock | O_NONBLOCK) < 0) { // Set non-blocking failed
        log_to_file("DNS: Error setting socket to non-blocking: %s", strerror(errno));
        close(dns_sock);
        return;
    } // End of set non-blocking failed

    char dns_server_ip_str[INET6_ADDRSTRLEN];
    uint16_t dns_server_port;
    if (upstream_dns_addr.ss_family == AF_INET) { // Block start for AF_INET
        dns_server_port = ntohs(((const struct sockaddr_in*)&upstream_dns_addr)->sin_port);
    } else { // AF_INET6
        dns_server_port = ntohs(((const struct sockaddr_in6*)&upstream_dns_addr)->sin6_port);
    } // Block end for port extraction

    // Extract client's source UDP port for logging from src_addr
    uint16_t client_udp_port;
    if (src_addr->ss_family == AF_INET) { // Block start for AF_INET
        client_udp_port = ntohs(((const struct sockaddr_in*)src_addr)->sin_port);
    } else { // AF_INET6
        client_udp_port = ntohs(((const struct sockaddr_in6*)src_addr)->sin6_port);
    } // Block end for port extraction

    log_to_file("DNS: Forwarding query ID %u from %s:%d to upstream DNS %s:%d (AF: %d, len: %d)",
                id, addr_to_string(src_addr, qname_str, sizeof(qname_str)), client_udp_port, 
                addr_to_string(&upstream_dns_addr, dns_server_ip_str, sizeof(dns_server_ip_str)), dns_server_port,
                upstream_dns_addr.ss_family, payload_len);


    ssize_t sent_bytes = sendto(dns_sock, payload, payload_len, 0,
                                (const struct sockaddr*)&upstream_dns_addr, upstream_dns_addr_len);
    if (sent_bytes < 0) { // Send failed
        log_to_file("DNS: Failed to send query to upstream DNS: %s (%s)", strerror(errno), qname_str); // Added errno
        close(dns_sock);
        return;
    } // End of send failed

    // Wait for response from upstream DNS (with a timeout)
    struct pollfd dns_poll_fd;
    dns_poll_fd.fd = dns_sock;
    dns_poll_fd.events = POLLIN;
    int poll_ret = poll(&dns_poll_fd, 1, 5000); // 5 seconds timeout

    if (poll_ret > 0 && (dns_poll_fd.revents & POLLIN)) { // Poll successful and readable
        char dns_response_buf[MAX_PACKET_SIZE];
        struct sockaddr_storage sender_dns_addr;
        socklen_t sender_dns_len = sizeof(sender_dns_addr);
        ssize_t recv_bytes = recvfrom(dns_sock, dns_response_buf, sizeof(dns_response_buf), 0,
                                      (struct sockaddr*)&sender_dns_addr, &sender_dns_len);

        if (recv_bytes > 0) { // Data received
            log_to_file("DNS: Received %zd bytes DNS response from upstream for query ID %u.", recv_bytes, id);
            
            // Extract the original client and destination ports for the response packet back to TUN
            // The dst_addr is the original DNS server address the client sent to.
            // The src_addr is the original client address.
            uint16_t client_original_src_port;
            uint16_t tun_dns_server_port; // This is the port on the TUN interface (usually 53)
            
            if (src_addr->ss_family == AF_INET) { // Block start for AF_INET
                client_original_src_port = ntohs(((const struct sockaddr_in*)src_addr)->sin_port);
                tun_dns_server_port = ntohs(((const struct sockaddr_in*)dst_addr)->sin_port);
            } else { // AF_INET6
                client_original_src_port = ntohs(((const struct sockaddr_in6*)src_addr)->sin6_port);
                tun_dns_server_port = ntohs(((const struct sockaddr_in6*)dst_addr)->sin6_port);
            } // Block end for port extraction

            struct udp_header reply_udph;
            reply_udph.uh_sport = htons(tun_dns_server_port); // Source port is DNS server (53) from TUN's perspective
            reply_udph.uh_dport = htons(client_original_src_port);         // Destination port is client's original source port
            reply_udph.uh_len = htons(sizeof(struct udp_header) + recv_bytes);
            reply_udph.uh_sum = 0; // Will be calculated by send_packet_to_tun

            // Send response back to TUN (from DNS server IP to client IP)
            // The 'dst_addr' for send_packet_to_tun is the DNS server's address (as seen by client on TUN)
            // The 'src_addr' for send_packet_to_tun is the client's address on TUN
            send_packet_to_tun(dst_addr, src_addr, IPPROTO_UDP,
                               &reply_udph, sizeof(reply_udph),
                               (uint8_t*)dns_response_buf, recv_bytes,
                               rand() & 0xFFFF);
            log_to_file("DNS: Response for ID %u sent back to TUN.", id);
        } else if (recv_bytes < 0 && errno != EAGAIN && errno != EWOULDBLOCK) { // Recv error
            log_to_file("DNS: Error receiving response from upstream DNS: %s (%u)", strerror(errno), id); // Added errno
        } else { // Timeout or no data
            log_to_file("DNS: Timeout or no data from upstream DNS for query ID %u.", id);
        } // End of recv_bytes check
    } else if (poll_ret == 0) { // Poll timeout
        log_to_file("DNS: Poll timeout waiting for upstream DNS response for query ID %u.", id);
    } else { // Poll error
        log_to_file("DNS: Poll error waiting for upstream DNS response: %s (%u)", strerror(errno), id); // Added errno
    } // End of poll_ret check

    close(dns_sock);
} // End of handle_dns_query


// מטפל בחבילות UDP נכנסות מה-TUN
void handle_udp_packet(const struct ip_header *iph, const struct ip6_hdr *ip6h,
                       const struct udp_header *udph_in, const uint8_t *payload, int payload_len, // Renamed udph to udph_in
                       sa_family_t addr_family) { // Function start
    char src_ip_str[INET6_ADDRSTRLEN], dst_ip_str[INET6_ADDRSTRLEN];
    struct sockaddr_storage src_addr_storage, dst_addr_storage; // Use _storage for writable buffer
    socklen_t src_addr_len, dst_addr_len;
    uint16_t src_port_host = ntohs(udph_in->uh_sport); // Use udph_in
    uint16_t dst_port_host = ntohs(udph_in->uh_dport); // Use udph_in

    if (addr_family == AF_INET) { // Block start for AF_INET
        struct sockaddr_in *s_src = (struct sockaddr_in *)&src_addr_storage;
        struct sockaddr_in *s_dst = (struct sockaddr_in *)&dst_addr_storage;
        memset(s_src, 0, sizeof(struct sockaddr_in));
        memset(s_dst, 0, sizeof(struct sockaddr_in));
        s_src->sin_family = AF_INET;
        s_dst->sin_family = AF_INET;
        s_src->sin_addr.s_addr = iph->ip_src;
        s_dst->sin_addr.s_addr = iph->ip_dst;
        s_src->sin_port = udph_in->uh_sport; // Use udph_in
        s_dst->sin_port = udph_in->uh_dport; // Use udph_in
        src_addr_len = sizeof(struct sockaddr_in);
        dst_addr_len = sizeof(struct sockaddr_in);
    } else { // AF_INET6
        struct sockaddr_in6 *s_src = (struct sockaddr_in6 *)&src_addr_storage; // No const
        struct sockaddr_in6 *s_dst = (struct sockaddr_in6 *)&dst_addr_storage; // No const
        memset((void*)s_src, 0, sizeof(struct sockaddr_in6)); // Cast to void* for old compilers
        memset((void*)s_dst, 0, sizeof(struct sockaddr_in6)); // Cast to void*
        s_src->sin6_family = AF_INET6;
        s_dst->sin6_family = AF_INET6;
        s_src->sin6_addr = ip6h->ip6_src;
        s_dst->sin6_addr = ip6h->ip6_dst;
        s_src->sin6_port = udph_in->uh_sport; // Use udph_in
        s_dst->sin6_port = udph_in->uh_dport; // Use udph_in
        src_addr_len = sizeof(struct sockaddr_in6);
        dst_addr_len = sizeof(struct sockaddr_in6);
    } // Block end for address family setup
    
    // Now pass the writable sockaddr_storage to sendto
    const struct sockaddr_storage *send_src_addr = &src_addr_storage;
    const struct sockaddr_storage *send_dst_addr = &dst_addr_storage;

    if (dst_port_host == 53) { // DNS packet (query to DNS server)
        log_to_file("UDP: Detected DNS query from %s:%d to %s:53",
                    addr_to_string(send_src_addr, src_ip_str, sizeof(src_ip_str)), src_port_host,
                    addr_to_string(send_dst_addr, dst_ip_str, sizeof(dst_ip_str)), dst_port_host);
        // Changed to call the DNS proxy function
        handle_dns_query(send_src_addr, send_dst_addr, payload, payload_len);
        return;
    } // End of DNS packet

    // עבור UDP רגיל, נשתמש באסטרטגיית שליחה/קבלה פשוטה במקום מצב סשן
    int sock = socket(addr_family, SOCK_DGRAM, 0); // SOCK_DGRAM עבור UDP
    if (sock < 0) { // Socket creation failed
        log_to_file("שגיאה: יצירת סוקט UDP עבור העברה נכשלה (AF: %d): %s", addr_family, strerror(errno));
        return;
    } // End of socket creation failed

    int flags = fcntl(sock, F_GETFL, 0);
    fcntl(sock, F_SETFL, flags | O_NONBLOCK);

    log_to_file("UDP מ-TUN: %s:%d -> %s:%d (אורך: %d, AF: %d)",
                addr_to_string(send_src_addr, src_ip_str, sizeof(src_ip_str)), src_port_host,
                addr_to_string(send_dst_addr, dst_ip_str, sizeof(dst_ip_str)), dst_port_host, payload_len, addr_family);

    ssize_t sent_len = sendto(sock, payload, payload_len, 0,
                              (const struct sockaddr*)send_dst_addr, dst_addr_len);
    if (sent_len < 0) { // Send failed
        log_to_file("שגיאה בשליחת נתוני UDP לאינטרנט: %s", strerror(errno));
    } else { // Send successful
        char response_buf[MAX_PACKET_SIZE];
        struct sockaddr_storage sender_addr;
        socklen_t sender_len = sizeof(sender_addr);
        ssize_t recv_len = recvfrom(sock, response_buf, sizeof(response_buf), 0,
                                    (struct sockaddr*)&sender_addr, &sender_len);

        if (recv_len > 0) { // Response received
            log_to_file("תשובת UDP התקבלה מהאינטרנט (%zd בתים).", recv_len);

            struct udp_header reply_udph;
            reply_udph.uh_sport = udph_in->uh_dport; // Use udph_in
            reply_udph.uh_dport = udph_in->uh_sport; // Use udph_in
            reply_udph.uh_len = htons(sizeof(struct udp_header) + recv_len);
            reply_udph.uh_sum = 0; 

            // Calculate UDP checksum, which is mandatory for IPv6. If it's 0 for IPv6, set to 0xFFFF.
            if (addr_family == AF_INET) { // Block start for AF_INET
                reply_udph.uh_sum = transport_checksum_ipv4(((const struct sockaddr_in *)send_dst_addr)->sin_addr.s_addr,
                                                             ((const struct sockaddr_in *)send_src_addr)->sin_addr.s_addr,
                                                             IPPROTO_UDP, &reply_udph, sizeof(reply_udph),
                                                             (uint8_t*)response_buf, recv_len);
            } else { // AF_INET6
                 reply_udph.uh_sum = transport_checksum_ipv6(&((const struct sockaddr_in6 *)send_dst_addr)->sin6_addr,
                                                             &((const struct sockaddr_in6 *)send_src_addr)->sin6_addr,
                                                             IPPROTO_UDP, sizeof(reply_udph) + recv_len,
                                                             &reply_udph, sizeof(reply_udph),
                                                             (uint8_t*)response_buf, recv_len);
                if (reply_udph.uh_sum == 0) {
                    reply_udph.uh_sum = 0xFFFF;
                }
            } // Block end for address family checksum

            send_packet_to_tun(send_dst_addr, send_src_addr, IPPROTO_UDP,
                               &reply_udph, sizeof(reply_udph),
                               (uint8_t*)response_buf, recv_len,
                               rand() & 0xFFFF);
        } else if (recv_len < 0 && errno != EAGAIN && errno != EWOULDBLOCK) { // Recv error
            log_to_file("שגיאה בקבלת תשובת UDP: %s", strerror(errno));
        } // End of recv_len check
    } // End of sent_len check
    close(sock);
} // End of handle_udp_packet


// מטפל בחבילות TCP נכנסות מה-TUN
void handle_tcp_packet(const struct ip_header *iph, const struct ip6_hdr *ip6h,
                       const struct tcp_header *tcph, const uint8_t *payload, int payload_len,
                       sa_family_t addr_family) { // Function start
    
    char src_ip_str[INET6_ADDRSTRLEN], dst_ip_str[INET6_ADDRSTRLEN];
    struct sockaddr_storage local_addr_storage, remote_addr_storage;
    socklen_t local_addr_len, remote_addr_len;
    uint16_t local_port_host = ntohs(tcph->th_sport);
    uint16_t remote_port_host = ntohs(tcph->th_dport);

    if (addr_family == AF_INET) { // Block start for AF_INET
        struct sockaddr_in *local_in = (struct sockaddr_in *)&local_addr_storage;
        struct sockaddr_in *remote_in = (struct sockaddr_in *)&remote_addr_storage;
        memset(local_in, 0, sizeof(struct sockaddr_in));
        memset(remote_in, 0, sizeof(struct sockaddr_in));
        local_in->sin_family = AF_INET;
        remote_in->sin_family = AF_INET;
        local_in->sin_addr.s_addr = iph->ip_src;
        remote_in->sin_addr.s_addr = iph->ip_dst;
        local_in->sin_port = tcph->th_sport;
        remote_in->sin_port = tcph->th_dport;
        local_addr_len = sizeof(struct sockaddr_in);
        remote_addr_len = sizeof(struct sockaddr_in);
    } else { // AF_INET6
        struct sockaddr_in6 *local_in6 = (struct sockaddr_in6 *)&local_addr_storage; // No const
        struct sockaddr_in6 *remote_in6 = (struct sockaddr_in6 *)&remote_addr_storage; // No const
        memset((void*)local_in6, 0, sizeof(struct sockaddr_in6)); // Cast to void* for old compilers
        memset((void*)remote_in6, 0, sizeof(struct sockaddr_in6)); // Cast to void*
        local_in6->sin6_family = AF_INET6;
        remote_in6->sin6_family = AF_INET6;
        local_in6->sin6_addr = ip6h->ip6_src;
        remote_in6->sin6_addr = ip6h->ip6_dst;
        local_in6->sin6_port = tcph->th_sport;
        remote_in6->sin6_port = tcph->th_dport;
        local_addr_len = sizeof(struct sockaddr_in6);
        remote_addr_len = sizeof(struct sockaddr_in6);
    } // Block end for address family setup

    const struct sockaddr_storage *session_local_addr = &local_addr_storage;
    const struct sockaddr_storage *session_remote_addr = &remote_addr_storage;

    tcp_session_t *session = find_session(session_local_addr, session_remote_addr);

    log_to_file("TCP מ-TUN: %s:%d -> %s:%d, Flags: 0x%x, Seq: %u, Ack: %u, PayloadLen: %d, State: %d, AF: %d",
                addr_to_string(session_local_addr, src_ip_str, sizeof(src_ip_str)), local_port_host,
                addr_to_string(session_remote_addr, dst_ip_str, sizeof(dst_ip_str)), remote_port_host,
                tcph->th_flags, ntohl(tcph->th_seq), ntohl(tcph->th_ack), payload_len,
                session ? session->state : TCP_STATE_CLOSED, addr_family);


    if (tcph->th_flags & TH_SYN) { // Block start for TH_SYN
        if (!session) { // No session found, create new
            session = create_session(session_local_addr, local_addr_len, session_remote_addr, remote_addr_len, addr_family);
            if (!session) { // Failed to create session
                log_to_file("שגיאה: נכשלה יצירת סשן עבור SYN.");
                return;
            }
        } // End of session creation

        log_to_file("TCP: SYN התקבל מלקוח TUN: %s:%d -> %s:%d (AF: %d)",
                    addr_to_string(session_local_addr, src_ip_str, sizeof(src_ip_str)), local_port_host,
                    addr_to_string(session_remote_addr, dst_ip_str, sizeof(dst_ip_str)), remote_port_host, addr_family);

        session->client_seq_num = ntohl(tcph->th_seq) + 1;
        session->server_ack_num = ntohl(tcph->th_ack); 
        
        uint16_t client_mss_val = extract_mss(tcph);
        if (client_mss_val == 0 || client_mss_val > VPN_RECOMMENDED_MSS) { // Adjust client MSS
            session->client_mss = VPN_RECOMMENDED_MSS;
            log_to_file("TCP: Client MSS adjusted from %u to %u", client_mss_val, session->client_mss);
        } else { // Use client MSS
            session->client_mss = client_mss_val;
        }

        // יצירת סוקט והתחברות לשרת האמיתי
        session->remote_socket_fd = create_and_connect_internet_socket(session_remote_addr, remote_addr_len, addr_family);
        if (session->remote_socket_fd < 0) { // Block start for remote_socket_fd < 0
            log_to_file("שגיאה: נכשל חיבור לשרת אמיתי עבור SYN. סוגר סשן.");
            
            // שליחת RST בחזרה ללקוח TUN
            struct tcp_header rst_tcph;
            memset(&rst_tcph, 0, sizeof(rst_tcph));
            rst_tcph.th_sport = tcph->th_dport; // פורט יעד מקורי (של השרת)
            rst_tcph.th_dport = tcph->th_sport; // פורט מקור מקורי (של הלקוח)
            rst_tcph.th_seq = htonl(session->server_seq_num); 
            rst_tcph.th_ack = htonl(session->client_seq_num); 
            rst_tcph.th_off = 5;
            rst_tcph.th_flags = TH_RST | TH_ACK; 
            rst_tcph.th_win = htons(0); 
            rst_tcph.th_sum = 0;
            if (addr_family == AF_INET) { // Block start for AF_INET
                 rst_tcph.th_sum = transport_checksum_ipv4(((const struct sockaddr_in *)session_remote_addr)->sin_addr.s_addr,
                                                           ((const struct sockaddr_in *)session_local_addr)->sin_addr.s_addr,
                                                           IPPROTO_TCP, &rst_tcph, sizeof(rst_tcph), NULL, 0);
            } else { // AF_INET6
                struct in6_addr temp_remote_in6_addr_copy = ((const struct sockaddr_in6 *)session_remote_addr)->sin6_addr;
                struct in6_addr temp_local_in6_addr_copy = ((const struct sockaddr_in6 *)session_local_addr)->sin6_addr;
                rst_tcph.th_sum = transport_checksum_ipv6(&temp_remote_in6_addr_copy, &temp_local_in6_addr_copy,
                                                          IPPROTO_TCP, sizeof(rst_tcph), &rst_tcph, sizeof(rst_tcph), NULL, 0);
            } // Block end for address family
            send_packet_to_tun(session_remote_addr, session_local_addr, IPPROTO_TCP, &rst_tcph, sizeof(rst_tcph), NULL, 0, ++session->ip_id_counter);
            
            close(session->remote_socket_fd);
            session->remote_socket_fd = -1; 
            session->state = TCP_STATE_CLOSED;
            return;
        } // Block end for remote_socket_fd < 0
        session->state = TCP_STATE_SYN_SENT; // עדכון מצב ל-SYN_SENT (ממתין להתחברות לשרת האמיתי)
        session->last_active = time(NULL);
        return; 
    } // Block end for TH_SYN

    if (!session) { // Block start for !session
        log_to_file("שגיאה: לא נמצא סשן עבור חבילת TCP נכנסת. שולח RST.");
        struct tcp_header rst_tcph;
        memset(&rst_tcph, 0, sizeof(rst_tcph));
        rst_tcph.th_sport = tcph->th_dport;
        rst_tcph.th_dport = tcph->th_sport;
        rst_tcph.th_seq = tcph->th_ack;
        uint32_t rst_ack = ntohl(tcph->th_seq) + payload_len;
        if (tcph->th_flags & (TH_SYN | TH_FIN)) { // Add 1 if SYN or FIN
            rst_ack++; 
        }
        rst_tcph.th_ack = htonl(rst_ack);

        rst_tcph.th_off = 5;
        rst_tcph.th_flags = TH_RST | TH_ACK; 
        rst_tcph.th_win = htons(0);
        rst_tcph.th_sum = 0;
        if (addr_family == AF_INET) { // Block start for AF_INET
            rst_tcph.th_sum = transport_checksum_ipv4(((const struct sockaddr_in *)session_remote_addr)->sin_addr.s_addr,
                                                      ((const struct sockaddr_in *)session_local_addr)->sin_addr.s_addr,
                                                      IPPROTO_TCP, &rst_tcph, sizeof(rst_tcph), NULL, 0);
        } else { // AF_INET6
            struct in6_addr temp_remote_in6_addr_copy = ((const struct sockaddr_in6 *)session_remote_addr)->sin6_addr;
            struct in6_addr temp_local_in6_addr_copy = ((const struct sockaddr_in6 *)session_local_addr)->sin6_addr;
            rst_tcph.th_sum = transport_checksum_ipv6(&temp_remote_in6_addr_copy, &temp_local_in6_addr_copy,
                                                      IPPROTO_TCP, sizeof(rst_tcph), &rst_tcph, sizeof(rst_tcph), NULL, 0);
        } // Block end for address family
        send_packet_to_tun(session_remote_addr, session_local_addr, IPPROTO_TCP, &rst_tcph, sizeof(rst_tcph), NULL, 0, rand() & 0xFFFF);
        return;
    } // Block end for !session

    session->last_active = time(NULL); 

    // עדכון client_seq_num - מספר הרצף הבא שה-VPN מצפה מהלקוח (ACK שאנו שולחים ללקוח)
    // ה-SEQ של הלקוח + אורך המטען. אם יש SYN/FIN, מוסיפים 1.
    if (payload_len > 0) { // Block start for payload_len > 0
        session->client_seq_num = ntohl(tcph->th_seq) + payload_len;
    } else { // No payload
        session->client_seq_num = ntohl(tcph->th_seq);
    } // End of payload_len check
    if (tcph->th_flags & (TH_SYN | TH_FIN)) { // If SYN or FIN flag is set
        session->client_seq_num++;
    }
    
    // עדכון server_ack_num - ה-ACK שאנחנו שולחים לשרת, מאשר את מה שקיבלנו מהלקוח
    session->server_ack_num = ntohl(tcph->th_seq) + payload_len;
    if (tcph->th_flags & (TH_SYN | TH_FIN)) { // If SYN or FIN flag is set
        session->server_ack_num++;
    }

    // עדכון server_seq_num - ה-ACK שהלקוח שלח לנו, מאשר את הנתונים שאנחנו שלחנו לו.
    if (tcph->th_flags & TH_ACK) { // If ACK flag is set
        if (ntohl(tcph->th_ack) > session->server_seq_num) { // Update if client ACK is newer
            session->server_seq_num = ntohl(tcph->th_ack);
        }
    } // End of ACK flag check


    // טיפול בחבילות ACK מהלקוח
    if (tcph->th_flags & TH_ACK) { // Block start for TH_ACK
        if (session->state == TCP_STATE_SYN_RECEIVED) { // State SYN_RECEIVED
            // הלקוח אישר את ה-SYN-ACK שלנו. החיבור בוסס.
            log_to_file("TCP: ACK ל-SYN-ACK התקבל מ-TUN. מצב: ESTABLISHED.");
            session->state = TCP_STATE_ESTABLISHED;
        } else if (session->state == TCP_STATE_FIN_WAIT_1) { // State FIN_WAIT_1
            // הלקוח שלח FIN, ועכשיו קיבל ACK על ה-FIN שלו מאיתנו
            log_to_file("TCP: ACK ל-FIN (שלנו) התקבל מ-TUN. מצב: FIN_WAIT_2.");
            session->state = TCP_STATE_FIN_WAIT_2;
        } else if (session->state == TCP_STATE_LAST_ACK) { // State LAST_ACK
            // הלקוח קיבל את ה-FIN שלנו, ועכשיו שולח ACK. סוגר סשן.
            log_to_file("TCP: ACK ל-FIN (של השרת) התקבל מ-TUN. סוגר סשן.");
            if (session->remote_socket_fd != -1) { // Close socket if open
                close(session->remote_socket_fd);
            }
            session->remote_socket_fd = -1;
            session->state = TCP_STATE_CLOSED;
            return; // אין צורך לעבד נתונים נוספים או לשלוח דגלים
        }
    } // Block end for TH_ACK


    if ((tcph->th_flags & TH_PUSH && payload_len > 0) || (payload_len > 0 && session->state == TCP_STATE_ESTABLISHED)) { // Block start for PUSH/data
        if (session->state == TCP_STATE_ESTABLISHED) { // Block start for ESTABLISHED state
            log_to_file("TCP: נתונים (PSH/ACK) התקבלו מ-TUN, אורך מטען: %d. מצב: ESTABLISHED. מעביר לשרת.", payload_len);

            if (session->hostname[0] == '\0') { // Block start for hostname extraction
                if (ntohs(remote_port_host) == 80) { // HTTP port
                    const char* extracted_hostname = extract_hostname_from_http_payload((const char*)payload, payload_len);
                    if (extracted_hostname) { // Hostname extracted
                        strncpy(session->hostname, extracted_hostname, sizeof(session->hostname) - 1);
                        session->hostname[sizeof(session->hostname) - 1] = '\0';
                        log_to_file("TCP: שם דומיין HTTP שחולץ עבור סשן: %s", session->hostname);
                        log_domain_to_file("HTTP Host: %s (from %s:%d)", session->hostname, 
                                           addr_to_string(session_local_addr, src_ip_str, sizeof(src_ip_str)), local_port_host);
                    }
                } else if (ntohs(remote_port_host) == 443) { // HTTPS port
                    const char* extracted_sni = extract_sni_hostname_from_tls_client_hello((const uint8_t*)payload, payload_len);
                    if (extracted_sni) { // SNI extracted
                        strncpy(session->hostname, extracted_sni, sizeof(session->hostname) - 1);
                        session->hostname[sizeof(session->hostname) - 1] = '\0';
                        log_to_file("TCP: שם דומיין SNI שחולץ עבור סשן HTTPS: %s", session->hostname);
                        log_domain_to_file("HTTPS SNI: %s (from %s:%d)", session->hostname, 
                                           addr_to_string(session_local_addr, src_ip_str, sizeof(src_ip_str)), local_port_host);
                    }
                }
            } // Block end for hostname extraction

            ssize_t sent_len = send(session->remote_socket_fd, payload, payload_len, 0);
            if (sent_len < 0 && errno != EAGAIN && errno != EWOULDBLOCK) { // Send failed
                log_to_file("שגיאה בשליחת נתונים לאינטרנט: %s", strerror(errno));
            } else if (sent_len > 0) { // Send successful
                 log_to_file("TCP: נשלחו %zd בתים לשרת האינטרנט.", sent_len);
            }
        } else if (payload_len > 0) { // נתונים התקבלו אך הסשן עדיין לא ESTABLISHED
             log_to_file("אזהרה: נתונים התקבלו במצב לא ESTABLISHED (%d). מעביר לשרת בכל זאת.", session->state);
             ssize_t sent_len = send(session->remote_socket_fd, payload, payload_len, 0);
             if (sent_len < 0 && errno != EAGAIN && errno != EWOULDBLOCK) { // Send failed
                 log_to_file("שגיאה בשליחת נתונים לאינטרנט (מצב לא ESTABLISHED): %s", strerror(errno));
             } else if (sent_len > 0) { // Send successful
                 log_to_file("TCP: נשלחו %zd בתים לשרת האינטרנט (מצב לא ESTABLISHED).", sent_len);
             }
        }
    } // Block end for PUSH/data

    if (tcph->th_flags & TH_FIN) { // Block start for TH_FIN
        log_to_file("TCP: FIN התקבל מ-TUN. מצב נוכחי: %d", session->state);
        
        if (session->state == TCP_STATE_ESTABLISHED) { // State ESTABLISHED
            log_to_file("TCP: הלקוח יזם FIN. מעדכן מצב ל-FIN_WAIT_1.");
            session->state = TCP_STATE_FIN_WAIT_1; 
            session->last_active = time(NULL);

        } else if (session->state == TCP_STATE_CLOSE_WAIT) { // State CLOSE_WAIT
            log_to_file("TCP: הלקוח קיבל FIN של השרת, כעת שולח FIN משלו. מעדכן מצב ל-LAST_ACK.");
            session->state = TCP_STATE_LAST_ACK;
            session->last_active = time(NULL);
        } else { // Unexpected state
            log_to_file("אזהרה: FIN התקבל במצב בלתי צפוי (%d).", session->state);
        }
    } // Block end for TH_FIN

    if (tcph->th_flags & TH_RST) { // Block start for TH_RST
        log_to_file("TCP: RST התקבל מ-TUN. סוגר סשן: %s:%d -> %s:%d (דומיין: %s)",
                    addr_to_string(session_local_addr, src_ip_str, sizeof(src_ip_str)), local_port_host,
                    addr_to_string(session_remote_addr, dst_ip_str, sizeof(dst_ip_str)), remote_port_host,
                    session->hostname[0] != '\0' ? session->hostname : "לא ידוע");
        if (session->remote_socket_fd != -1) { // Close socket if open
            close(session->remote_socket_fd);
        }
        session->remote_socket_fd = -1; 
        session->state = TCP_STATE_CLOSED;
    } // Block end for TH_RST
} // End of handle_tcp_packet


// מטפל בחבילת IP נכנסת מהתקן TUN
void handle_ip_packet(const char *buf, ssize_t len) { // Function start
    if (len < sizeof(struct ip_header)) { // Packet too short
        log_to_file("שגיאה: חבילה קצרה מדי כדי להיות כותרת IP.");
        return;
    } // End of packet too short

    // Determine IP version
    uint8_t ip_version = (buf[0] >> 4);
    uint8_t protocol;
    const char *transport_header_start;
    int transport_payload_len;
    sa_family_t addr_family;

    const struct ip_header *iph = NULL;
    const struct ip6_hdr *ip6h = NULL;

    if (ip_version == 4) { // Block start for IPv4
        iph = (const struct ip_header*)buf;
        if (len < sizeof(struct ip_header)) { // Check min IPv4 header size
            log_to_file("שגיאה: חבילת IPv4 קצרה מדי עבור כותרת.");
            return;
        }
        int ip_header_len = iph->ip_hl * 4;
        if (len < ip_header_len) { // Check total packet length vs header length
            log_to_file("שגיאה: אי התאמה באורך כותרת IPv4.");
            return;
        }
        log_pkt_to_file(buf + ip_header_len);
        protocol = iph->ip_p;
        transport_header_start = buf + ip_header_len;
        transport_payload_len = len - ip_header_len;
        addr_family = AF_INET;
    } else if (ip_version == 6) { // Block start for IPv6
        ip6h = (const struct ip6_hdr*)buf;
        if (len < sizeof(struct ip6_hdr)) { // Check min IPv6 header size
            log_to_file("שגיאה: חבילת IPv6 קצרה מדי עבור כותרת.");
            return;
        }
        // ip6_plen is only the payload length, not total length, so we use it for transport_payload_len
        // Total packet len is sizeof(ip6_hdr) + ip6_plen
        // We trust the `len` passed to this function as the total bytes read from TUN
        
        protocol = ip6h->ip6_nxt;
        transport_header_start = buf + sizeof(struct ip6_hdr);
        transport_payload_len = len - sizeof(struct ip6_hdr); // This is `ip6_plen` from the header if packet is complete
        addr_family = AF_INET6;
    } else { // Block start for unknown IP version
        log_to_file("שגיאה: גרסת IP לא נתמכת: %d", ip_version);
        return;
    } // Block end for IP version handling

    switch (protocol) { // Block start for protocol switch
        case IPPROTO_TCP:
            if (transport_payload_len < sizeof(struct tcp_header)) { // TCP header too short
                log_to_file("שגיאה: חבילת TCP קצרה מדי עבור כותרת.");
                return;
            }
            struct tcp_header *tcph = (struct tcp_header*)transport_header_start;
            handle_tcp_packet(iph, ip6h, tcph, (const uint8_t*)(transport_header_start + (tcph->th_off * 4)),
                              transport_payload_len - (tcph->th_off * 4), addr_family);
            break;
        case IPPROTO_UDP:
            if (transport_payload_len < sizeof(struct udp_header)) { // UDP header too short
                log_to_file("שגיאה: חבילת UDP קצרה מדי עבור כותרת.");
                return;
            }
            struct udp_header *udph_passed = (struct udp_header*)transport_header_start; // Renamed udph to udph_passed
            handle_udp_packet(iph, ip6h, udph_passed, (uint8_t*)(transport_header_start + sizeof(struct udp_header)),
                              transport_payload_len - sizeof(struct udp_header), addr_family);
            break;
        case IPPROTO_ICMP:
            log_to_file("ICMPv4 התקבל. כרגע לא מטופל.");
            break;
        case IPPROTO_ICMPV6: // Handle ICMPv6 as well
            log_to_file("ICMPv6 התקבל. כרגע לא מטופל.");
            break;
        case 0: // IPv6 Hop-by-Hop Options Header (might indicate fragmentation or other options)
            log_to_file("פרוטוקול IP לא ידוע 0 (ייתכן ש-IPv6 Hop-by-Hop Options Header). מתעלם.");
            break;
        default:
            log_to_file("פרוטוקול IP לא ידוע %d. מתעלם.", protocol);
            break;
    } // Block end for protocol switch
} // End of handle_ip_packet

*/







static pcapdroid_t *global_pd = NULL;
static pthread_t jni_thread;

jni_classes_t cls;
jni_methods_t mids;
jni_fields_t fields;
jni_enum_t enums;
bool block_private_dns = false;
/* ******************************************************* */
JNIEXPORT void JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_setdebug(JNIEnv *env, jclass clazz, jboolean enabled) {
        debug = enabled;
}
JNIEXPORT jint JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_initLogger(JNIEnv *env, jclass clazz,
                                                             jstring path, jint level) {
    const char *path_s = (*env)->GetStringUTFChars(env, path, 0);
    int rv = pd_init_logger(path_s, level);
    (*env)->ReleaseStringUTFChars(env, path, path_s);
    return rv;
}
JNIEXPORT jint JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_writeLog(JNIEnv *env, jclass clazz,
                                                      jint logger, jint lvl, jstring message) {
    if(!debug)
        return -1;
    const char *message_s = (*env)->GetStringUTFChars(env, message, 0);
    int rv = pd_log_write(logger, lvl, message_s);
    (*env)->ReleaseStringUTFChars(env, message, message_s);
    return rv;
}
JNIEXPORT jint JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_getNumCheckedMalwareConnections(JNIEnv *env, jclass clazz) {
    return bl_num_checked_connections;
}

/* ******************************************************* */

JNIEXPORT jint JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_getNumCheckedFirewallConnections(JNIEnv *env, jclass clazz) {
    return fw_num_checked_connections;
}
JNIEXPORT void JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_setPrivateDnsBlocked(JNIEnv *env, jclass clazz, jboolean to_block) {
    block_private_dns = to_block;
}
JNIEXPORT void JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_reloadBlacklists(JNIEnv *env, jclass clazz) {
    reload_blacklists_now = true;
}
JNIEXPORT jint JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_getFdSetSize(JNIEnv *env, jclass clazz) {
    return FD_SETSIZE;
}
JNIEXPORT void JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_stopPacketLoop(JNIEnv *env, jclass type) {
    /* NOTE: the select on the packets loop uses a timeout to wake up periodically */
    log_i("stopPacketLoop called");
    running = false;
}
JNIEXPORT void JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_setDnsServer(JNIEnv *env, jclass clazz,
                                                               jstring server) {
    struct in_addr addr = {0};
    const char *value = (*env)->GetStringUTFChars(env, server, 0);

    if(inet_aton(value, &addr) != 0)
        new_dns_server = addr.s_addr;

    (*env)->ReleaseStringUTFChars(env, server, value);
}
static void log_callback(int lvl, const char *line) {
    pcapdroid_t *pd = global_pd;

    if(!debug)
        return;
    
    // quick path for debug logs
   // if(lvl < PD_DEFAULT_LOGGER_LEVEL)
    //    return;

    pd_log_write(PD_DEFAULT_LOGGER, lvl, line);

    // ensure that we are invoking jni from the attached thread
    if(!pd || !(pthread_equal(jni_thread, pthread_self())))
        return;

    if(lvl >= ANDROID_LOG_FATAL) {
        // This is a fatal error, report it to the gui
        jobject info_string = (*pd->env)->NewStringUTF(pd->env, line);

        if((jniCheckException(pd->env) != 0) || (info_string == NULL))
            return;

        (*pd->env)->CallVoidMethod(pd->env, pd->capture_service, mids.reportError, info_string);
        jniCheckException(pd->env);

        (*pd->env)->DeleteLocalRef(pd->env, info_string);
    }
}
void pd_refresh_time(pcapdroid_t *pd) {
    struct timespec ts;

    if(clock_gettime(CLOCK_MONOTONIC_COARSE, &ts)) {
        log_to_file("clock_gettime failed[%d]: %s", errno, strerror(errno));
        return;
    }

    pd->now_ms = (uint64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}
static struct timeval* get_pkt_timestamp(pcapdroid_t *pd, struct timeval *tv) {
    struct timespec ts;

    if(!clock_gettime(CLOCK_REALTIME, &ts)) {
        tv->tv_sec = ts.tv_sec;
        tv->tv_usec = ts.tv_nsec / 1000;
        return tv;
    }

    log_to_file("clock_gettime failed[%d]: %s", errno, strerror(errno));
    return tv;
}
void pd_init_pkt_context(pkt_context_t *pctx,
                         zdtun_pkt_t *pkt, bool is_tx, const zdtun_5tuple_t *tuple,
                         pd_conn_t *data, struct timeval *tv
) {
    pctx->pkt = pkt;
    pctx->tv = *tv;
    pctx->ms = (uint64_t)tv->tv_sec * 1000 + tv->tv_usec / 1000;
    pctx->is_tx = is_tx;
    pctx->tuple = tuple;
    pctx->data = data;
    pctx->plain_data = NULL; // managed by capture_libpcap
}
void getApplicationByUid(pcapdroid_t *pd, jint uid, char *buf, int bufsize) {
    JNIEnv *env = pd->env;
    const char *value = NULL;

    jstring obj = (*env)->CallObjectMethod(env, pd->capture_service, mids.getApplicationByUid, uid);
    jniCheckException(env);

    if(obj)
        value = (*env)->GetStringUTFChars(env, obj, 0);

    if(value)
        snprintf(buf, bufsize, "%s", value);
    else
        snprintf(buf, bufsize, "???");

    if(value) (*env)->ReleaseStringUTFChars(env, obj, value);
    if(obj) (*env)->DeleteLocalRef(env, obj);
}
char* get_appname_by_uid(pcapdroid_t *pd, int uid, char *buf, int bufsize) {
#ifdef ANDROID
    uid_to_app_t *app_entry;

    HASH_FIND_INT(pd->uid2app, &uid, app_entry);
    if(app_entry == NULL) {
        app_entry = (uid_to_app_t*) pd_malloc(sizeof(uid_to_app_t));

        if(app_entry) {
            // Resolve the app name
            getApplicationByUid(pd, uid, app_entry->appname, sizeof(app_entry->appname));

            //log_to_file("uid %d resolved to \"%s\"", uid, app_entry->appname);

            app_entry->uid = uid;
            HASH_ADD_INT(pd->uid2app, uid, app_entry);
        }
    }
#else
    uid_to_app_t *app_entry = NULL;
#endif

    if(app_entry) {
        strncpy(buf, app_entry->appname, bufsize-1);
        buf[bufsize-1] = '\0';
    } else
        buf[0] = '\0';

    return buf;
}
static void process_payload(pcapdroid_t *pd, pkt_context_t *pctx) {
    const zdtun_pkt_t *pkt = pctx->pkt;
    pd_conn_t *data = pctx->data;
    bool truncated = data->payload_truncated;
    bool updated = false;

    if((pd->payload_mode == PAYLOAD_MODE_NONE) ||
       (pd->cb.dump_payload_chunk == NULL) ||
       (pkt->l7_len <= 0) ||
       (data->has_decrypted_data && !pctx->plain_data) ||
       (pd->tls_decryption.enabled && data->proxied)) // NOTE: when performing TLS decryption, TCP connections data is handled by the MitmReceiver
        return;

    if((pd->payload_mode != PAYLOAD_MODE_MINIMAL) || !data->has_payload[pctx->is_tx]) {
        const char *to_dump;
        int dump_size;

        if (pctx->plain_data) {
            // if there is plaintext (decrypted) data, dump it instead of the encrypted data
            to_dump = (const char*) pctx->plain_data->data;
            dump_size = pctx->plain_data->data_len;

            if (!data->has_decrypted_data) {
                // existing chunks are encrypted, so drop them
                if (pd->cb.clear_payload_chunks)
                    pd->cb.clear_payload_chunks(pd, pctx);
                data->has_decrypted_data = true;
            }
        } else {
            to_dump = pkt->l7;
            dump_size = pkt->l7_len;
        }

        if((pd->payload_mode == PAYLOAD_MODE_MINIMAL) && (dump_size > MINIMAL_PAYLOAD_MAX_DIRECTION_SIZE)) {
            dump_size = MINIMAL_PAYLOAD_MAX_DIRECTION_SIZE;
            truncated = true;
        }

        if(pd->cb.dump_payload_chunk(pd, pctx, to_dump, dump_size)) {
            data->has_payload[pctx->is_tx] = true;
            updated = true;
        } else
            truncated = true;
    } else
        truncated = true;

    if((updated && data->payload_chunks) || (truncated != data->payload_truncated)) {
        data->payload_truncated |= truncated;
        data->update_type |= CONN_UPDATE_PAYLOAD;
        pd_notify_connection_update(pd, pctx->tuple, data);
    }
}
struct ndpi_detection_module_struct* init_ndpi() {
#ifdef FUZZING
    // nDPI initialization is very expensive, cache it
    // see also ndpi_exit_detection_module
    static struct ndpi_detection_module_struct *ndpi_cache = NULL;

    if(ndpi_cache != NULL)
      return ndpi_cache;
#endif

    struct ndpi_detection_module_struct *ndpi = ndpi_init_detection_module(NULL);
    NDPI_PROTOCOL_BITMASK protocols;

    if(!ndpi)
        return(NULL);

    // needed by pd_get_proto_name
    if(!masterProtosInit) {
        init_ndpi_protocols_bitmask(&masterProtos);
        masterProtosInit = true;
    }

#ifndef FUZZING
    // enable all the protocols
    NDPI_BITMASK_SET_ALL(protocols);
#else
    // nDPI has a big performance impact on fuzzing.
    // Only enable some protocols to extract the metadata for use in
    // PCAPdroid, we are not fuzzing nDPI!
    NDPI_BITMASK_RESET(protocols);
    NDPI_BITMASK_ADD(protocols, NDPI_PROTOCOL_DNS);
    NDPI_BITMASK_ADD(protocols, NDPI_PROTOCOL_HTTP);
    //NDPI_BITMASK_ADD(protocols, NDPI_PROTOCOL_TLS);
#endif

    ndpi_set_protocol_detection_bitmask2(ndpi, &protocols);

    ndpi_finalize_initialization(ndpi);

#ifdef FUZZING
    ndpi_cache = ndpi;
#endif

    return(ndpi);
}
uint16_t pd_ndpi2proto(ndpi_protocol nproto) {
    // The nDPI master/app protocol logic is not clear (e.g. the first packet of a DNS flow has
    // master_protocol unknown whereas the second has master_protocol set to DNS). We are not interested
    // in the app protocols, so just take the one that's not unknown.
    uint16_t l7proto = ((nproto.proto.master_protocol != NDPI_PROTOCOL_UNKNOWN) ?
            nproto.proto.master_protocol : nproto.proto.app_protocol);

    if((l7proto == NDPI_PROTOCOL_HTTP_CONNECT) || (l7proto == NDPI_PROTOCOL_HTTP_PROXY))
        l7proto = NDPI_PROTOCOL_HTTP;

    if(!masterProtosInit) {
        init_ndpi_protocols_bitmask(&masterProtos);
        masterProtosInit = true;
    }

    // nDPI will still return a disabled protocol (via the bitmask) if it matches some
    // metadata for it (e.g. the SNI)
    if(!NDPI_ISSET(&masterProtos, l7proto))
        l7proto = NDPI_PROTOCOL_UNKNOWN;

    //log_d("PROTO: %d/%d -> %d", proto.master_protocol, proto.app_protocol, l7proto);

    return l7proto;
}

static bool is_encrypted_l7(struct ndpi_detection_module_struct *ndpi_str, uint16_t l7proto) {
    // The ndpi_is_encrypted_proto API does not work reliably as it mixes master protocols with apps
    if(l7proto >= (NDPI_MAX_SUPPORTED_PROTOCOLS + NDPI_MAX_NUM_CUSTOM_PROTOCOLS))
        return false;

    ndpi_proto_defaults_t *proto_defaults = ndpi_get_proto_defaults(ndpi_str);
    return (proto_defaults && (proto_defaults[l7proto].isClearTextProto == 0));
}
static void process_dns_reply(pd_conn_t *data, pcapdroid_t *pd, const struct zdtun_pkt *pkt) {
    const char *query = (const char*) data->ndpi_flow->host_server_name;

    if((!query[0]) || !strchr(query, '.') || (pkt->l7_len < sizeof(dns_packet_t)))
        return;

    dns_packet_t *dns = (dns_packet_t*)pkt->l7;

    if(((ntohs(dns->flags) & 0x8000) == 0x8000) && (dns->questions != 0) && (dns->answ_rrs != 0)) {
        u_char *reply = dns->queries;
        int len = pkt->l7_len - sizeof(dns_packet_t);
        int num_queries = ntohs(dns->questions);
        int num_replies = min(ntohs(dns->answ_rrs), 32);

        // Skip queries
        for(int i=0; (i<num_queries) && (len > 0); i++) {
            while((len > 0) && (*reply != '\0')) {
                reply++;
                len--;
            }

            reply += 5; len -= 5;
        }

        for(int i=0; (i<num_replies) && (len > 0); i++) {
            int ipver = 0;
            zdtun_ip_t rsp_addr = {0};

            // Skip name
            while(len > 0) {
                if(*reply == 0x00) {
                    reply++; len--;
                    break;
                } else if(*reply == 0xc0) {
                    reply+=2; len-=2;
                    break;
                }

                reply++; len--;
            }

            if(len < 10)
                return;

            uint16_t rec_type = ntohs((*(uint16_t*)reply));
            uint16_t addr_len = ntohs((*(uint16_t*)(reply + 8)));
            reply += 10; len -= 10;

            if (len < addr_len)
                return;

            if((rec_type == 0x1) && (addr_len == 4)) { // A record
                ipver = 4;
                rsp_addr.ip4 = *((u_int32_t*)reply);
            } else if((rec_type == 0x1c) && (addr_len == 16)) { // AAAA record
                ipver = 6;
                memcpy(&rsp_addr.ip6, reply, 16);
            }
            //log_to_file("ipver=%d",ipver);
            if(ipver != 0) {
                char rspip[INET6_ADDRSTRLEN];
                int family = (ipver == 4) ? AF_INET : AF_INET6;

                rspip[0] = '\0';
                inet_ntop(family, &rsp_addr, rspip, sizeof(rspip));
                //new
                //log_to_file("792 [v%d]: %s -> %s %s\n",ipver, rspip, query," only dns");
                //end new
                log_to_file("Host LRU cache ADD [v%d]: %s -> %s", ipver, rspip, query);
                ip_lru_add(pd->ip_to_host, &rsp_addr, query);
            }

            reply += addr_len; len -= addr_len;
        }
    }
}
static bool is_numeric_host(const char *host) {
    if(isdigit(*host))
        return true;

    for(; *host; host++) {
        char ch = *host;

        if(ch == ':') // IPv6
            return true;
        if(ch == '.')
            break;
    }

    return false;
}
static bool matches_decryption_whitelist(pcapdroid_t *pd, const zdtun_5tuple_t *tuple, pd_conn_t *data) {
    zdtun_ip_t dst_ip = tuple->dst_ip;

    if(!pd->tls_decryption.list)
        return false;

    // NOTE: domain matching only works if a prior DNS reply is seen (see ip_lru_find in pd_new_connection)
    return blacklist_match_ip(pd->tls_decryption.list, &dst_ip, tuple->ipver) ||
        blacklist_match_uid(pd->tls_decryption.list, data->uid) ||
        (data->info && blacklist_match_domain(pd->tls_decryption.list, data->info));
}
static bool should_proxify(pcapdroid_t *pd, const zdtun_5tuple_t *tuple, pd_conn_t *data) {
    if(!pd->socks5.enabled)
        return false;
//log_to_file("11");
    if (pd->tls_decryption.list) {
//  log_to_file("12");
        // TLS decryption
        if(!matches_decryption_whitelist(pd, tuple, data)) {
//log_to_file("13");
            data->decryption_ignored = true;
            return false;
        }
//log_to_file("14");
        // Since we cannot reliably determine TLS connections with 1 packet, and connections must be
        // proxified on the 1st packet, we proxify all the TCP connections
    }
    
    //FILE *fp=fopen("/storage/emulated/0/logpcapa.txt","a");
        //fprintf(fp,"%d %d%s\n",6,6,"");
        //fclose(fp);
    log_to_file("400%d",tuple->ipproto == IPPROTO_TCP);
    //old
    return (tuple->ipproto == IPPROTO_TCP);
    //new
    //return true;
}

static void check_blacklisted_domain(pcapdroid_t *pd, pd_conn_t *data, const zdtun_5tuple_t *tuple) {
    if(data->info && data->info[0]) {
        if(pd->malware_detection.bl && !data->blacklisted_domain && !data->whitelisted_app) {
            bool blacklisted = blacklist_match_domain(pd->malware_detection.bl, data->info);
            /*old
            if(blacklisted) {
                char appbuf[64];
                char buf[512];
                get_appname_by_uid(pd, data->uid, appbuf, sizeof(appbuf));

                // Check if whitelisted
                if(pd->malware_detection.whitelist && blacklist_match_domain(pd->malware_detection.whitelist, data->info))
                    log_d("Whitelisted domain [%s]: %s [%s]", data->info,
                          zdtun_5tuple2str(tuple, buf, sizeof(buf)), appbuf);
                else {
                    log_w("Blacklisted domain [%s]: %s [%s]", data->info,
                          zdtun_5tuple2str(tuple, buf, sizeof(buf)), appbuf);
                    data->blacklisted_domain = true;
                    data->to_block = true;
                }
            }*/
            /*new*/
            domainopen=false;
            if(!blacklisted) {
                char appbuf[64];
                char buf[512];
                get_appname_by_uid(pd, data->uid, appbuf, sizeof(appbuf));

                // Check if whitelisted
                if(pd->malware_detection.whitelist && blacklist_match_domain(pd->malware_detection.whitelist, data->info)){
                    log_d("new Whitelisted domain [%s]: %s [%s]", data->info,
                          zdtun_5tuple2str(tuple, buf, sizeof(buf)), appbuf);
                             //new
                        log_to_file("343 %s %s",data->info ," open");
                        //end new
                          if(thischeck) domainopen=true;
                          thischeck=false;
                }
                else {
                    log_w("new Blacklisted domain [%s]: %s [%s]", data->info,
                          zdtun_5tuple2str(tuple, buf, sizeof(buf)), appbuf);
                          //new
                        log_to_file("355 %s %s",data->info ," block");
                        //end new
                    data->blacklisted_domain = true;
                    data->to_block = true;
                    char bufb[512];
                    //log_to_file("363 m yes [%s]: %s [%s]\n", data->info,
                         // zdtun_5tuple2str(tuple, bufb, sizeof(bufb)), appbuf);
                }
            }else{
               //new
        log_to_file("371 %s %s",data->info ," open");
        //end new
                  if(thischeck) domainopen=true;
                  thischeck=false;
            }
            /*new end*/
        }

        if(pd->firewall.enabled && pd->firewall.bl && !data->to_block) {
            // Check if the domain is explicitly blocked by the firewall
            data->to_block |= blacklist_match_domain(pd->firewall.bl, data->info);
            if(data->to_block) {
                char appbuf[64];
                char buf[512];

                get_appname_by_uid(pd, data->uid, appbuf, sizeof(appbuf));
                log_d("Blocked domain [%s]: %s [%s]", data->info, zdtun_5tuple2str(tuple, buf, sizeof(buf)), appbuf);
            }
        }
    }
}

/* ******************************************************* */

static void check_whitelist_mode_block(pcapdroid_t *pd, const zdtun_5tuple_t *tuple, pd_conn_t *data) {
    // whitelist mode: block any app unless it's explicitly whitelisted.
    // The blocklist still has priority to determine if a connection should be blocked.

    // NOTE: data->l7proto is not computed yet
    bool is_dns = (tuple->ipproto == IPPROTO_UDP) && (ntohs(tuple->dst_port) == 53);

    if(pd->firewall.enabled && pd->firewall.wl_enabled && pd->firewall.wl && !data->to_block &&
            // always allow DNS traffic from unspecified apps
            (!is_dns || ((data->uid != UID_NETD) && (data->uid != UID_PHONE) && (data->uid != UID_UNKNOWN))))
        data->to_block = !blacklist_match_uid(pd->firewall.wl, data->uid);
    /*new block unknown (firewall whitelist mode)*/
    if(data->uid == UID_UNKNOWN) data->to_block = true;//solution1a
    /*end new*/
}
void vpn_process_ndpi(pcapdroid_t *pd, const zdtun_5tuple_t *tuple, pd_conn_t *data) {
    if(data->l7proto == NDPI_PROTOCOL_QUIC) {
        block_quic_mode_t block_mode = pd->vpn.block_quic_mode;

        if ((block_mode == BLOCK_QUIC_MODE_ALWAYS) ||
                ((block_mode == BLOCK_QUIC_MODE_TO_DECRYPT) && matches_decryption_whitelist(pd, tuple, data))) {
            data->blacklisted_internal = true;
            data->to_block = true;
        }
    }

    if(block_private_dns && !data->to_block &&
            (data->l7proto == NDPI_PROTOCOL_TLS) &&
            data->info && blacklist_match_domain(pd->vpn.known_dns_servers, data->info)) {
        log_to_file("blocking connection to private DNS server %s", data->info);
        data->blacklisted_internal = true;
        data->to_block = true;
    }
}

/* ******************************************************* */

static void process_ndpi_data(pcapdroid_t *pd, const zdtun_5tuple_t *tuple, pd_conn_t *data) {
    char *found_info = NULL;

    switch(data->l7proto) {
        case NDPI_PROTOCOL_TLS:
            // ALPN extension in client hello (https://datatracker.ietf.org/doc/html/rfc7301)
            if(!data->alpn && data->ndpi_flow->protos.tls_quic.negotiated_alpn) {
                if(strstr(data->ndpi_flow->protos.tls_quic.negotiated_alpn, "http/")) {
                    data->alpn = NDPI_PROTOCOL_HTTP;
                    data->update_type |= CONN_UPDATE_INFO;
                } else if(strstr(data->ndpi_flow->protos.tls_quic.negotiated_alpn, "imap")) {
                    data->alpn = NDPI_PROTOCOL_MAIL_IMAP;
                    data->update_type |= CONN_UPDATE_INFO;
                } else if(strstr(data->ndpi_flow->protos.tls_quic.negotiated_alpn, "stmp")) {
                    data->alpn = NDPI_PROTOCOL_MAIL_SMTP;
                    data->update_type |= CONN_UPDATE_INFO;
                } else {
                    log_to_file("Unknown ALPN: %s", data->ndpi_flow->protos.tls_quic.negotiated_alpn);
                    data->alpn = NDPI_PROTOCOL_TLS; // mark to avoid port-based guessing
                }
            }
            /* fallthrough */
        case NDPI_PROTOCOL_DNS:
            if(data->ndpi_flow->host_server_name[0])
                found_info = (char*)data->ndpi_flow->host_server_name;
            break;
        case NDPI_PROTOCOL_HTTP:
            if(data->ndpi_flow->host_server_name[0] &&
               !is_numeric_host((char*)data->ndpi_flow->host_server_name))
                found_info = (char*)data->ndpi_flow->host_server_name;

            if(!data->url && data->ndpi_flow->http.url) {
                data->url = pd_strndup(data->ndpi_flow->http.url, 256);
                data->update_type |= CONN_UPDATE_INFO;
            }

            break;
    }
    //log_to_file("info = %s",found_info);
    if(found_info && (!data->info || data->info_from_lru)) {
        if(data->info)
            pd_free(data->info);
        data->info = pd_strndup(found_info, 256);
        data->info_from_lru = false;
        //new
        //log_to_file("639 %s %s\n",data->info ," only dns or http");
        //end new
        check_blacklisted_domain(pd, data, tuple);
        data->update_type |= CONN_UPDATE_INFO;
    }

    if(pd->vpn_capture)
        vpn_process_ndpi(pd, tuple, data);
}
static void conn_free_ndpi(pd_conn_t *data) {
    if(data->ndpi_flow) {
        ndpi_free_flow(data->ndpi_flow);
        data->ndpi_flow = NULL;
    }
}
/* ******************************************************* */

/* Stop the DPI detection and determine the l7proto of the connection. */
void pd_giveup_dpi(pcapdroid_t *pd, pd_conn_t *data, const zdtun_5tuple_t *tuple) {
    if(!data->ndpi_flow)
        return;

    if(data->l7proto == NDPI_PROTOCOL_UNKNOWN) {
        uint8_t proto_guessed;
        struct ndpi_proto n_proto = ndpi_detection_giveup(pd->ndpi, data->ndpi_flow,
                              &proto_guessed);
        data->l7proto = pd_ndpi2proto(n_proto);
        data->encrypted_l7 = is_encrypted_l7(pd->ndpi, data->l7proto);
    }

    log_d("nDPI completed[pkts=%d, ipver=%d, proto=%d] -> l7proto: %d",
                data->sent_pkts + data->rcvd_pkts,
                tuple->ipver, tuple->ipproto, data->l7proto);

    process_ndpi_data(pd, tuple, data);
    conn_free_ndpi(data);
}
static void perform_dpi(pcapdroid_t *pd, pkt_context_t *pctx) {
    pd_conn_t *data = pctx->data;
    bool giveup = ((data->sent_pkts + data->rcvd_pkts + 1) >= MAX_DPI_PACKETS);
    zdtun_pkt_t *pkt = pctx->pkt;
    bool is_tx = pctx->is_tx;

    uint16_t old_proto = data->l7proto;
    struct ndpi_proto n_proto = ndpi_detection_process_packet(pd->ndpi, data->ndpi_flow, (const u_char *)pkt->buf,
                                  pkt->len, data->last_seen, NULL);
    data->l7proto = pd_ndpi2proto(n_proto);

    if(old_proto != data->l7proto) {
        data->update_type |= CONN_UPDATE_INFO;
        data->encrypted_l7 = is_encrypted_l7(pd->ndpi, data->l7proto);
    }

    if(!is_tx && (data->l7proto == NDPI_PROTOCOL_DNS))
        process_dns_reply(data, pd, pkt);

    if(giveup || ((data->l7proto != NDPI_PROTOCOL_UNKNOWN) &&
            !ndpi_extra_dissection_possible(pd->ndpi, data->ndpi_flow)))
        pd_giveup_dpi(pd, data, &pkt->tuple); // calls process_ndpi_data
    else
        process_ndpi_data(pd, &pkt->tuple, data);

    if((data->l7proto == NDPI_PROTOCOL_DNS)
       && (data->uid == UID_NETD)
       && (data->sent_pkts + data->rcvd_pkts == 0)
       && ((netd_resolve_waiting > 0) || ((next_connections_dump - NETD_RESOLVE_DELAY_MS) < pd->now_ms))) {
        if(netd_resolve_waiting == 0) {
            // Wait before sending the dump to possibly resolve netd DNS connections uid.
            // Only delay for the first DNS request, to avoid excessive delay.
           // log_to_file("Adding netd resolution delay");
            next_connections_dump += NETD_RESOLVE_DELAY_MS;
        }
        netd_resolve_waiting++;
    }

    if(!data->ndpi_flow) {
        // nDPI detection complete
        if((data->l7proto == NDPI_PROTOCOL_TLS) && (!data->alpn)) {
            if(ntohs(pctx->tuple->dst_port) == 443)
                data->alpn = NDPI_PROTOCOL_HTTP; // assume HTTPS
            else if(data->info && !strncmp(data->info, "imap.", 5))
                data->alpn = NDPI_PROTOCOL_MAIL_IMAP; // assume IMAPS
            else if(data->info && !strncmp(data->info, "smtp.", 5))
                data->alpn = NDPI_PROTOCOL_MAIL_SMTP; // assume SMTPS

            if(data->alpn) {
                data->update_type |= CONN_UPDATE_INFO;
                pd_notify_connection_update(pd, pctx->tuple, data);
            }
        }
    }
}

void pd_process_packet(pcapdroid_t *pd, pkt_context_t *pctx) {
    pd_conn_t *data = pctx->data;
    zdtun_pkt_t *pkt = pctx->pkt;

    // NOTE: pd_account_stats will not be called for blocked connections
    data->last_seen = pctx->ms;
    if(!data->first_seen)
        data->first_seen = pctx->ms;

    if(data->ndpi_flow &&
       (!(pkt->flags & ZDTUN_PKT_IS_FRAGMENT) || (pkt->flags & ZDTUN_PKT_IS_FIRST_FRAGMENT))) {
        // nDPI cannot handle fragments, since they miss the L4 layer (see ndpi_iph_is_valid_and_not_fragmented)
        perform_dpi(pd, pctx);
    }

    if (pctx->plain_data && (data->alpn != NDPI_PROTOCOL_UNKNOWN) && (data->alpn != pctx->data->l7proto)) {
        // we have the L7 decrypted data
        pd_giveup_dpi(pd, data, pctx->tuple);
        pctx->data->l7proto = data->alpn;

        data->update_type |= CONN_UPDATE_INFO;
        pd_notify_connection_update(pd, pctx->tuple, data);
    }

    process_payload(pd, pctx);
}
static int notif_connection(pcapdroid_t *pd, conn_array_t *arr, const zdtun_5tuple_t *tuple, pd_conn_t *data) {
    // End the detection when the connection is closed
    // Always check this, even pending_notification are present
    if(data->status >= CONN_STATUS_CLOSED)
        pd_giveup_dpi(pd, data, tuple);

    if(data->pending_notification)
        return 0;

    if(arr->cur_items >= arr->size) {
        /* Extend array */
        arr->size = (arr->size == 0) ? 8 : (arr->size * 2);
        arr->items = pd_realloc(arr->items, arr->size * sizeof(conn_and_tuple_t));

        if(arr->items == NULL) {
            log_to_file("realloc(conn_array_t) (%d items) failed", arr->size);
            return -1;
        }
    }

    conn_and_tuple_t *slot = &arr->items[arr->cur_items++];
    slot->tuple = *tuple;
    slot->data = data;
    data->pending_notification = true;
    return 0;
}

static jobject getConnUpdate(pcapdroid_t *pd, const conn_and_tuple_t *conn) {
    JNIEnv *env = pd->env;
    pd_conn_t *data = conn->data;

    jobject update = (*env)->NewObject(env, cls.conn_update, mids.connUpdateInit, data->incr_id);

    if((update == NULL) || jniCheckException(env)) {
        log_e("NewObject(ConnectionDescriptor) failed");
        return NULL;
    }

    if(data->update_type & CONN_UPDATE_STATS) {
        bool blocked = data->to_block && pd->vpn_capture; // currently can only block connections in non-root mode

        (*env)->CallVoidMethod(env, update, mids.connUpdateSetStats, data->last_seen,
                               data->payload_length, data->sent_bytes, data->rcvd_bytes, data->sent_pkts, data->rcvd_pkts, data->blocked_pkts,
                               (data->tcp_flags[0] << 8) | data->tcp_flags[1],
                               (data->error << 16) /* 8 bits are enough for socket errno */ |
                                    (data->port_mapping_applied << 13) |
                                    (data->decryption_ignored << 12) |
                                    (data->netd_block_missed << 11) |
                                    (blocked << 10) |
                                    (data->blacklisted_domain << 9) |
                                    (data->blacklisted_ip << 8) |
                                    (data->status & 0xFF) /* 8 bits */);
    }
    if(data->update_type & CONN_UPDATE_INFO) {
        jobject info = (*env)->NewStringUTF(env, data->info ? data->info : "");
        jobject url = (*env)->NewStringUTF(env, data->url ? data->url : "");
        jobject l7proto = (*env)->NewStringUTF(env, pd_get_proto_name(pd, data->l7proto, data->alpn,
                                                                      conn->tuple.ipproto));
        int flags = data->encrypted_l7;

        (*env)->CallVoidMethod(env, update, mids.connUpdateSetInfo, info, url, l7proto, flags);

        (*env)->DeleteLocalRef(env, info);
        (*env)->DeleteLocalRef(env, url);
        (*env)->DeleteLocalRef(env, l7proto);
    }
    if(data->update_type & CONN_UPDATE_PAYLOAD) {
       // (*env)->CallVoidMethod(env, update, mids.connUpdateSetPayload, data->payload_chunks,
                     //          data->payload_truncated |
                         //      (data->has_decrypted_data << 1));
        (*pd->env)->DeleteLocalRef(pd->env, data->payload_chunks);
        data->payload_chunks = NULL;
    }

    // reset the update flag
    data->update_type = 0;

    if(jniCheckException(env)) {
        log_e("getConnUpdate() failed");
        (*env)->DeleteLocalRef(env, update);
        return NULL;
    }

    return update;
}
/*
void pcap_iter_connections(pcapdroid_t *pd, conn_cb cb) {
    pcap_conn_t *conn, *tmp;

    HASH_ITER(hh, pd->pcap.connections, conn, tmp) {
        if(cb(pd, &conn->tuple, conn->data) != 0)
            return;
    }
}*/
static int dumpNewConnection(pcapdroid_t *pd, const conn_and_tuple_t *conn, jobject arr, int idx) {
    char srcip[INET6_ADDRSTRLEN], dstip[INET6_ADDRSTRLEN];
    JNIEnv *env = pd->env;
    const zdtun_5tuple_t *conn_info = &conn->tuple;
    const pd_conn_t *data = conn->data;
    int rv = 0;
    int family = (conn->tuple.ipver == 4) ? AF_INET : AF_INET6;

    if((inet_ntop(family, &conn_info->src_ip, srcip, sizeof(srcip)) == NULL) ||
       (inet_ntop(family, &conn_info->dst_ip, dstip, sizeof(dstip)) == NULL)) {
        log_w("inet_ntop failed: ipver=%d, dstport=%d", conn->tuple.ipver, ntohs(conn_info->dst_port));
        return 0;
    }

#if 0
    log_i( "DUMP: [proto=%d]: %s:%u -> %s:%u [%d]",
                        conn_info->ipproto,
                        srcip, ntohs(conn_info->src_port),
                        dstip, ntohs(conn_info->dst_port),
                        data->uid);
#endif

    jobject src_string = (*env)->NewStringUTF(env, srcip);
    jobject dst_string = (*env)->NewStringUTF(env, dstip);
    jobject country_code = (*env)->NewStringUTF(env, data->country_code);
    u_int ifidx = (pd->vpn_capture ? 0 : data->pcap.ifidx);
    u_int local_port = (pd->vpn_capture ? data->vpn.local_port : conn_info->src_port);
    bool mitm_decrypt = (pd->tls_decryption.enabled && data->proxied);
    jobject conn_descriptor = (*env)->NewObject(env, cls.conn, mids.connInit, data->incr_id,
                                                conn_info->ipver, conn_info->ipproto,
                                                src_string, dst_string, country_code,
                                                ntohs(conn_info->src_port), ntohs(conn_info->dst_port),
                                                ntohs(local_port),
                                                data->uid, ifidx, mitm_decrypt, data->first_seen);

    if((conn_descriptor != NULL) && !jniCheckException(env)) {
        // This is the first update, send all the data
        conn->data->update_type |= CONN_UPDATE_STATS | CONN_UPDATE_INFO;
        jobject update = getConnUpdate(pd, conn);

        if(update != NULL) {
            (*env)->CallVoidMethod(env, conn_descriptor, mids.connProcessUpdate, update);
            (*env)->DeleteLocalRef(env, update);
        } else
            rv = -1;

        /* Add the connection to the array */
        (*env)->SetObjectArrayElement(env, arr, idx, conn_descriptor);

        if(jniCheckException(env))
            rv = -1;

        (*env)->DeleteLocalRef(env, conn_descriptor);
    } else {
        log_e("NewObject(ConnectionDescriptor) failed");
        rv = -1;
    }

    (*env)->DeleteLocalRef(env, src_string);
    (*env)->DeleteLocalRef(env, dst_string);
    (*env)->DeleteLocalRef(env, country_code);

    return rv;
}

/* ******************************************************* */

static int dumpConnectionUpdate(pcapdroid_t *pd, const conn_and_tuple_t *conn, jobject arr, int idx) {
    JNIEnv *env = pd->env;
    jobject update = getConnUpdate(pd, conn);

    if(update != NULL) {
        (*env)->SetObjectArrayElement(env, arr, idx, update);
        (*env)->DeleteLocalRef(env, update);
        return 0;
    }

    return -1;
}

static void notifyBlacklistsLoaded(pcapdroid_t *pd, bl_status_arr_t *status_arr) {
    JNIEnv *env = pd->env;
    jobject status_obj = (*env)->NewObjectArray(env, status_arr->cur_items, cls.blacklist_status, NULL);

    if((status_obj == NULL) || jniCheckException(env)) {
        log_e("NewObjectArray() failed");
        return;
    }

    for(int i=0; i<status_arr->cur_items; i++) {
        bl_status_t *st = &status_arr->items[i];
        jstring fname = (*env)->NewStringUTF(env, st->fname);
        if((fname == NULL) || jniCheckException(env))
            break;

        jobject stats = (*env)->NewObject(env, cls.blacklist_status, mids.blacklistStatusInit,
                                              fname, st->num_rules);
        (*env)->DeleteLocalRef(env, fname);

        if((stats == NULL) || jniCheckException(env))
            break;

        (*env)->SetObjectArrayElement(env, status_obj, i, stats);
        (*env)->DeleteLocalRef(env, stats);

        if(jniCheckException(env)) {
            break;
        }
    }

    (*env)->CallVoidMethod(env, pd->capture_service, mids.notifyBlacklistsLoaded, status_obj);
    (*env)->DeleteLocalRef(env, status_obj);
}

pd_conn_t* pd_new_connection(pcapdroid_t *pd, const zdtun_5tuple_t *tuple, int uid) {
    pd_conn_t *data = pd_calloc(1, sizeof(pd_conn_t));
    if(!data) {
        log_e("calloc(pd_conn_t) failed with code %d/%s",
                    errno, strerror(errno));
        return(NULL);
    }

    /* nDPI */
    if((data->ndpi_flow = ndpi_calloc(1, SIZEOF_FLOW_STRUCT)) == NULL) {
        log_to_file("ndpi_flow_malloc failed");
        pd_purge_connection(pd, data);
        return(NULL);
    }
    //log_to_file("ndpi_flow_malloc success.%s",data->info);
    if(notif_connection(pd, &pd->new_conns, tuple, data) < 0) {
        pd_purge_connection(pd, data);
        return(NULL);
    }

    data->uid = uid;
    data->incr_id = pd->new_conn_id++;

    if(pd->malware_detection.whitelist) {
        // NOTE: if app is whitelisted, no need to check for blacklisted IP/domains
        data->whitelisted_app = blacklist_match_uid(pd->malware_detection.whitelist, uid);

        if(data->whitelisted_app) {
            char appbuf[64];
            char buf[256];
            get_appname_by_uid(pd, data->uid, appbuf, sizeof(appbuf));

           // log_to_file("Whitelisted app: %s [%s]", zdtun_5tuple2str(tuple, buf, sizeof(buf)), appbuf);
        }
    }

    // Query country info
    const zdtun_ip_t dst_ip = tuple->dst_ip;
    char remote_ip[INET6_ADDRSTRLEN];
    int family = (tuple->ipver == 4) ? AF_INET : AF_INET6;

    remote_ip[0] = '\0';
    inet_ntop(family, &dst_ip, remote_ip, sizeof(remote_ip));

#ifdef ANDROID
   // getCountryCode(pd, remote_ip, data->country_code);
#endif

    // Try to resolve host name via the LRU cache
    data->info = ip_lru_find(pd->ip_to_host, &dst_ip);
      //  log_to_file("cache lru host %s",data->info);
    //new
    //print app name before the mitm the ip and host
        
    char appbufb[64];
    char bufb[256];
    get_appname_by_uid(pd, data->uid, appbufb, sizeof(appbufb));
    //log_to_file("447 %d %s [%s] %s , ", data->uid, zdtun_5tuple2str(tuple, bufb, sizeof(bufb)), appbufb,"");
  
    //end new
    if(data->info) {
        //new
              
       // log_to_file("439 %s %s %s\n",remote_ip, data->info,"");
        //end new
       // log_to_file("Host LRU cache HIT: %s -> %s", remote_ip, data->info);
        data->info_from_lru = true;

        if(data->uid != UID_UNKNOWN) {
            // When a DNS request is followed by a TLS connection or similar, mark the DNS request
            // with the uid of this connection. This allows us to match netd requests to actual apps.
            // Only change the uid of new connections (pd->new_conns) to avoid possible side effects
            for(int i=0; i < pd->new_conns.cur_items; i++) {
                conn_and_tuple_t *conn = &pd->new_conns.items[i];

                if((conn->data->uid == UID_NETD)
                        && (conn->data->info != NULL)
                        && (strcmp(conn->data->info, data->info) == 0)) {
                    char buf[256];

                    conn->data->uid = data->uid;

                    if(!conn->data->to_block && pd->firewall.enabled && pd->firewall.bl && (
                            blacklist_match_uid(pd->firewall.bl, conn->data->uid) ||
                            (pd->firewall.wl_enabled && pd->firewall.wl && !blacklist_match_uid(pd->firewall.wl, conn->data->uid))))
                        conn->data->netd_block_missed = true;

                    zdtun_5tuple2str(&conn->tuple, buf, sizeof(buf));
                    log_d("Resolved netd uid: %s : %d", buf, data->uid);

                    if(netd_resolve_waiting > 0) {
                        // If all the netd connections have been resolved, remove the dump delay
                        if((--netd_resolve_waiting) == 0) {
                            log_d("Removing netd resolution delay");
                            next_connections_dump -= NETD_RESOLVE_DELAY_MS;
                        }
                    }
                }
            }
        }
        /*new only true domainopen here. not in all use of domain check*/
        thischeck = true;
        /*end new solution1b*/
        check_blacklisted_domain(pd, data, tuple);
    } 
    //new
    else{
        //log_to_file("492 %s %s\n",remote_ip,"");
       
    }
    //end new

    if(pd->malware_detection.bl) {
        if(!data->whitelisted_app) {
            bool blacklisted = blacklist_match_ip(pd->malware_detection.bl, &dst_ip, tuple->ipver);
            /*old
            if (blacklisted) {
                char appbuf[64];
                char buf[256];
                get_appname_by_uid(pd, data->uid, appbuf, sizeof(appbuf));

                if(pd->malware_detection.whitelist && blacklist_match_ip(pd->malware_detection.whitelist, &dst_ip, tuple->ipver))
                    log_d("Whitelisted dst ip: %s [%s]", zdtun_5tuple2str(tuple, buf, sizeof(buf)),
                          appbuf);
                else {
                    log_w("Blacklisted dst ip: %s [%s]", zdtun_5tuple2str(tuple, buf, sizeof(buf)), appbuf);
                    data->blacklisted_ip = true;
                    data->to_block = true;
                }
            }*/
            /*new*/
            if (!blacklisted&&!domainopen) {
                char appbuf[64];
                char buf[256];
                get_appname_by_uid(pd, data->uid, appbuf, sizeof(appbuf));

                if(pd->malware_detection.whitelist && blacklist_match_ip(pd->malware_detection.whitelist, &dst_ip, tuple->ipver))
                    log_d("new Whitelisted dst ip: %s [%s]", zdtun_5tuple2str(tuple, buf, sizeof(buf)),
                          appbuf);
                else {
                    log_w("new Blacklisted dst ip: %s [%s]", zdtun_5tuple2str(tuple, buf, sizeof(buf)), appbuf);
                    data->blacklisted_ip = true;
                    data->to_block = true;
                }
            }
            domainopen=false;//end checking domain must implement very important (solution1b)
            /*end new*/
        }

        bl_num_checked_connections++;
    }

    if(pd->firewall.enabled && pd->firewall.bl && !data->to_block) {
        char appbuf[64];
        char buf[256];

        data->to_block |= blacklist_match_ip(pd->firewall.bl, &dst_ip, tuple->ipver);
        if(data->to_block) {
            get_appname_by_uid(pd, data->uid, appbuf, sizeof(appbuf));
            //log_to_file("Blocked ip: %s [%s]", zdtun_5tuple2str(tuple, buf, sizeof(buf)), appbuf);
        }

        if(!data->to_block) {
            data->to_block = blacklist_match_uid(pd->firewall.bl, data->uid);
            if(data->to_block) {
                get_appname_by_uid(pd, data->uid, appbuf, sizeof(appbuf));
               // log_to_file("Blocked app: %s [%s]", zdtun_5tuple2str(tuple, buf, sizeof(buf)), appbuf);
            }
        }

        if(!data->to_block) {
            data->to_block = blacklist_match_country(pd->firewall.bl, data->country_code);
            if(data->to_block) {
                get_appname_by_uid(pd, data->uid, appbuf, sizeof(appbuf));
                //log_to_file("Blocked country \"%s\": %s [%s]", data->country_code,
                   //   zdtun_5tuple2str(tuple, buf, sizeof(buf)), appbuf);
            }
        }

        fw_num_checked_connections++;
    }

    check_whitelist_mode_block(pd, tuple, data);

    return(data);
}
static void getSocks5ProxyAuth(pcapdroid_t *pd) {
    char buf[64];
    buf[0] = '\0';

    getStringPref(pd, "getSocks5ProxyAuth", buf, sizeof(buf));
    char *sep = strchr(buf, ':');

    if(!sep)
        return;

    *sep = '\0';
    strncpy(pd->socks5.proxy_user, buf, sizeof(pd->socks5.proxy_user));
    strncpy(pd->socks5.proxy_pass, sep + 1, sizeof(pd->socks5.proxy_pass));

    //log_d("SOCKS5: user=%s pass=%s", pd->socks5.proxy_user, pd->socks5.proxy_pass);
}
static void init_jni(JNIEnv *env) {
    // NOTE: these are bound to this specific env

    /* Classes */
    cls.vpn_service = jniFindClass(env, "com/emanuelef/remote_capture/CaptureService");
    cls.conn = jniFindClass(env, "com/emanuelef/remote_capture/model/ConnectionDescriptor");
    cls.conn_update = jniFindClass(env, "com/emanuelef/remote_capture/model/ConnectionUpdate");
    cls.stats = jniFindClass(env, "com/emanuelef/remote_capture/model/CaptureStats");
    cls.blacklist_status = jniFindClass(env, "com/emanuelef/remote_capture/Blacklists$NativeBlacklistStatus");
    cls.blacklist_descriptor = jniFindClass(env, "com/emanuelef/remote_capture/model/BlacklistDescriptor");
    cls.matchlist_descriptor = jniFindClass(env, "com/emanuelef/remote_capture/model/MatchList$ListDescriptor");
    cls.list = jniFindClass(env, "java/util/List");
    cls.arraylist = jniFindClass(env, "java/util/ArrayList");
    cls.payload_chunk = jniFindClass(env, "com/emanuelef/remote_capture/model/PayloadChunk");

    /* Methods */
    mids.reportError = jniGetMethodID(env, cls.vpn_service, "reportError", "(Ljava/lang/String;)V");
    mids.getApplicationByUid = jniGetMethodID(env, cls.vpn_service, "getApplicationByUid", "(I)Ljava/lang/String;"),
    mids.getPackageNameByUid = jniGetMethodID(env, cls.vpn_service, "getPackageNameByUid", "(I)Ljava/lang/String;"),
    mids.loadUidMapping = jniGetMethodID(env, cls.vpn_service, "loadUidMapping", "(ILjava/lang/String;Ljava/lang/String;)V"),
    mids.getCountryCode = jniGetMethodID(env, cls.vpn_service, "getCountryCode", "(Ljava/lang/String;)Ljava/lang/String;"),
    mids.protect = jniGetMethodID(env, cls.vpn_service, "protect", "(I)Z");
    mids.dumpPcapData = jniGetMethodID(env, cls.vpn_service, "dumpPcapData", "([B)V");
    mids.stopPcapDump = jniGetMethodID(env, cls.vpn_service, "stopPcapDump", "()V");
    mids.updateConnections = jniGetMethodID(env, cls.vpn_service, "updateConnections", "([Lcom/emanuelef/remote_capture/model/ConnectionDescriptor;[Lcom/emanuelef/remote_capture/model/ConnectionUpdate;)V");
    mids.sendStatsDump = jniGetMethodID(env, cls.vpn_service, "sendStatsDump", "(Lcom/emanuelef/remote_capture/model/CaptureStats;)V");
    mids.sendServiceStatus = jniGetMethodID(env, cls.vpn_service, "sendServiceStatus", "(Ljava/lang/String;)V");
    mids.getLibprogPath = jniGetMethodID(env, cls.vpn_service, "getLibprogPath", "(Ljava/lang/String;)Ljava/lang/String;");
    mids.notifyBlacklistsLoaded = jniGetMethodID(env, cls.vpn_service, "notifyBlacklistsLoaded", "([Lcom/emanuelef/remote_capture/Blacklists$NativeBlacklistStatus;)V");
    mids.getBlacklistsInfo = jniGetMethodID(env, cls.vpn_service, "getBlacklistsInfo", "()[Lcom/emanuelef/remote_capture/model/BlacklistDescriptor;");
    mids.connInit = jniGetMethodID(env, cls.conn, "<init>", "(IIILjava/lang/String;Ljava/lang/String;Ljava/lang/String;IIIIIZJ)V");
    mids.connProcessUpdate = jniGetMethodID(env, cls.conn, "processUpdate", "(Lcom/emanuelef/remote_capture/model/ConnectionUpdate;)V");
    mids.connUpdateInit = jniGetMethodID(env, cls.conn_update, "<init>", "(I)V");
    mids.connUpdateSetStats = jniGetMethodID(env, cls.conn_update, "setStats", "(JJJJIIIII)V");
    mids.connUpdateSetInfo = jniGetMethodID(env, cls.conn_update, "setInfo", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
    mids.connUpdateSetPayload = jniGetMethodID(env, cls.conn_update, "setPayload", "(Ljava/util/ArrayList;I)V");
    mids.statsInit = jniGetMethodID(env, cls.stats, "<init>", "()V");
    mids.statsSetData = jniGetMethodID(env, cls.stats, "setData", "(Ljava/lang/String;JJJJJIIIIIIIII)V");
    mids.blacklistStatusInit = jniGetMethodID(env, cls.blacklist_status, "<init>", "(Ljava/lang/String;I)V");
    mids.listSize = jniGetMethodID(env, cls.list, "size", "()I");
    mids.listGet = jniGetMethodID(env, cls.list, "get", "(I)Ljava/lang/Object;");
    mids.arraylistNew = jniGetMethodID(env, cls.arraylist, "<init>", "()V");
    mids.arraylistAdd = jniGetMethodID(env, cls.arraylist, "add", "(Ljava/lang/Object;)Z");
    mids.payloadChunkInit = jniGetMethodID(env, cls.payload_chunk, "<init>", "([BLcom/emanuelef/remote_capture/model/PayloadChunk$ChunkType;ZJ)V");

    /* Fields */
    fields.bldescr_fname = jniFieldID(env, cls.blacklist_descriptor, "fname", "Ljava/lang/String;");
    fields.bldescr_type = jniFieldID(env, cls.blacklist_descriptor, "type", "Lcom/emanuelef/remote_capture/model/BlacklistDescriptor$Type;");
    fields.ld_apps = jniFieldID(env, cls.matchlist_descriptor, "apps", "Ljava/util/List;");
    fields.ld_hosts = jniFieldID(env, cls.matchlist_descriptor, "hosts", "Ljava/util/List;");
    fields.ld_ips = jniFieldID(env, cls.matchlist_descriptor, "ips", "Ljava/util/List;");
    fields.ld_countries = jniFieldID(env, cls.matchlist_descriptor, "countries", "Ljava/util/List;");

    /* Enums */
    enums.bltype_ip = jniEnumVal(env, "com/emanuelef/remote_capture/model/BlacklistDescriptor$Type", "IP_BLACKLIST");
    enums.chunktype_raw = jniEnumVal(env, "com/emanuelef/remote_capture/model/PayloadChunk$ChunkType", "RAW");
    enums.chunktype_http = jniEnumVal(env, "com/emanuelef/remote_capture/model/PayloadChunk$ChunkType", "HTTP");
}


void pd_purge_connection(pcapdroid_t *pd, pd_conn_t *data) {
    if(!data)
        return;

    conn_free_ndpi(data);

    if(data->info)
        pd_free(data->info);
    if(data->url)
        pd_free(data->url);

#ifdef ANDROID
    if(data->payload_chunks)
        (*pd->env)->DeleteLocalRef(pd->env, data->payload_chunks);
#endif

    pd_free(data);
}
void pd_account_stats(pcapdroid_t *pd, pkt_context_t *pctx) {
    zdtun_pkt_t *pkt = pctx->pkt;
    pd_conn_t *data = pctx->data;

    data->payload_length += pkt->l7_len;

    if(pctx->is_tx) {
        data->sent_pkts++;
        data->sent_bytes += pkt->len;
        pd->capture_stats.sent_pkts++;
        pd->capture_stats.sent_bytes += pkt->len;
        if(pkt->tuple.ipver == 6) {
            pd->capture_stats.ipv6_sent_bytes += pkt->len;
        }
    } else {
        data->rcvd_pkts++;
        data->rcvd_bytes += pkt->len;
        pd->capture_stats.rcvd_pkts++;
        pd->capture_stats.rcvd_bytes += pkt->len;
        if(pkt->tuple.ipver == 6) {
            pd->capture_stats.ipv6_rcvd_bytes += pkt->len;
        }
    }

    /* New stats to notify */
    pd->capture_stats.new_stats = true;
    data->update_type |= CONN_UPDATE_STATS;
    pd_notify_connection_update(pd, pctx->tuple, pctx->data);

   /* if((pd->pcap_dump.dumper) &&
            ((pd->pcap_dump.max_pkts_per_flow <= 0) ||
                ((data->sent_pkts + data->rcvd_pkts) <= pd->pcap_dump.max_pkts_per_flow))) {
        u_int ifidx = !pd->vpn_capture ? pctx->data->pcap.ifidx : 0;
        pd_dump_packet(pd, pkt->buf, pkt->len, &pctx->tv, pctx->data->uid, ifidx);
    }*/
}
static int remote2vpn(zdtun_t *zdt, zdtun_pkt_t *pkt, const zdtun_conn_t *conn_info) {
    if(!running)
        // e.g. during zdtun_finalize
        return 0;

    pcapdroid_t *pd = (pcapdroid_t*) zdtun_userdata(zdt);
    const zdtun_5tuple_t *tuple = zdtun_conn_get_5tuple(conn_info);
    pd_conn_t *data = zdtun_conn_get_userdata(conn_info);

    // if this is called inside zdtun_forward, account the egress packet before the subsequent ingress packet
    if(data->vpn.fw_pctx) {
        pd_account_stats(pd, data->vpn.fw_pctx);
        data->vpn.fw_pctx = NULL;
    }

    struct timeval tv;
    pkt_context_t pctx;
    pd_refresh_time(pd);

    pd_init_pkt_context(&pctx, pkt, false, tuple, data, get_pkt_timestamp(pd, &tv));
    pd_process_packet(pd, &pctx);
    if(data->to_block) {
        data->blocked_pkts++;
        data->update_type |= CONN_UPDATE_STATS;
        pd_notify_connection_update(pd, tuple, data);

        // Returning -1 will result into an error condition on the connection, forcing a connection
        // close. Closing the connection is mandatory as it's not possible to handle dropped packets
        // via zdtun, since data received via the zdtun TCP sockets must be delivered to the client.
        return -1;
    }
    //new
    //char *buff=pkt->buf;
    //log_to_file("buf- %s",buff);
    //end new
    int rv = write(pd->vpn.tunfd, pkt->buf, pkt->len);
    if(rv < 0) {
        if(errno == ENOBUFS) {
            char buf[256];

            // Do not abort, the connection will be terminated
            log_to_file("Got ENOBUFS %s", zdtun_5tuple2str(tuple, buf, sizeof(buf)));
        } else if(errno == EIO) {
            log_to_file("Got I/O error (terminating?)");
            running = false;
        } else {
            log_to_file("zdt write (%d) failed [%d]: %s", pkt->len, errno, strerror(errno));
            running = false;
        }
    } else if(rv != pkt->len) {
        log_to_file("partial zdt write (%d / %d)", rv, pkt->len);
        rv = -1;
    } else {
        // Success
        rv = 0;
        pd_account_stats(pd, &pctx);
    }

    return rv;
}
static void update_conn_status(zdtun_t *zdt, const zdtun_pkt_t *pkt, uint8_t from_tun, const zdtun_conn_t *conn_info) {
    pd_conn_t *data = zdtun_conn_get_userdata(conn_info);

    // Update the connection status
    data->status = zdtun_conn_get_status(conn_info);
    if(data->status >= CONN_STATUS_CLOSED) {
        data->to_purge = true;
        data->error = zdtun_conn_get_error(conn_info);
    }
}
static int resolve_uid(pcapdroid_t *pd, const zdtun_5tuple_t *conn_info) {
    char buf[256];
    jint uid;

    zdtun_5tuple2str(conn_info, buf, sizeof(buf));
    uid = get_uid(pd->vpn.resolver, conn_info);

    if(uid >= 0) {
        char appbuf[64];

        get_appname_by_uid(pd, uid, appbuf, sizeof(appbuf));
        //log_to_file( "%s [%d/%s]", buf, uid, appbuf);
    } else {
        uid = UID_UNKNOWN;
        log_to_file("%s => UID not found!", buf);
    }

    return(uid);
}
static void protectSocketCallback(zdtun_t *zdt, socket_t sock) {
#if ANDROID
    pcapdroid_t *pd = ((pcapdroid_t*)zdtun_userdata(zdt));
    JNIEnv *env = pd->env;

    if(!pd->vpn_capture)
        return;

    /* Call VpnService protect */
    jboolean isProtected = (*env)->CallBooleanMethod(
            env, pd->capture_service, mids.protect, sock);
    jniCheckException(env);

    if(!isProtected)
        log_to_file("socket protect failed");
#endif
}

static int handle_new_connection(zdtun_t *zdt, zdtun_conn_t *conn_info) {
    pcapdroid_t *pd = ((pcapdroid_t *) zdtun_userdata(zdt));
    const zdtun_5tuple_t *tuple = zdtun_conn_get_5tuple(conn_info);

    pd_conn_t *data = pd_new_connection(pd, tuple, resolve_uid(pd, tuple));
    if(!data) {
        /* reject connection */
        return (1);
    }

    zdtun_conn_set_userdata(conn_info, data);

    /* accept connection */
    return(0);
}



/* Call this when the connection data has changed. The connection data will be sent to JAVA during the
 * next sendConnectionsDump. The type of change is determined by the data->update_type.
 * A negative value is returned if the connection update could not be enqueued. */
int pd_notify_connection_update(pcapdroid_t *pd, const zdtun_5tuple_t *tuple, pd_conn_t *data) {
    return notif_connection(pd, &pd->conns_updates, tuple, data);
}
static void connection_closed(zdtun_t *zdt, const zdtun_conn_t *conn_info) {
    pcapdroid_t *pd = (pcapdroid_t*) zdtun_userdata(zdt);
    pd_conn_t *data = zdtun_conn_get_userdata(conn_info);

    if(!data) {
        log_to_file("Missing data in connection");
        return;
    }

    const zdtun_5tuple_t *tuple = zdtun_conn_get_5tuple(conn_info);

    // Send last notification
    // Will free the data in sendConnectionsDump
    data->update_type |= CONN_UPDATE_STATS;
    if(pd_notify_connection_update(pd, tuple, data) < 0) {
        pd_purge_connection(pd, data);
        return;
    }

    pd_giveup_dpi(pd, data, tuple);
    data->status = zdtun_conn_get_status(conn_info);
    data->error = zdtun_conn_get_error(conn_info);
    data->to_purge = true;
}
static bool arraylist_add_string(JNIEnv *env, jmethodID arrayListAdd, jobject arr, const char *s) {
    jobject s_obj = (*env)->NewStringUTF(env, s);
    if(!s_obj || jniCheckException(env))
        return false;

    bool rv = (*env)->CallBooleanMethod(env, arr, arrayListAdd, s_obj);
    (*env)->DeleteLocalRef(env, s_obj);
    return rv;
}
JNIEXPORT jobject JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_getL7Protocols(JNIEnv *env, jclass clazz) {
    jclass arrayListClass = jniFindClass(env, "java/util/ArrayList");
    jmethodID arrayListNew = jniGetMethodID(env, arrayListClass, "<init>", "()V");
    jmethodID arrayListAdd = jniGetMethodID(env, arrayListClass, "add", "(Ljava/lang/Object;)Z");

    struct ndpi_detection_module_struct *ndpi = ndpi_init_detection_module(NULL);
    if(!ndpi)
        return(NULL);

    NDPI_PROTOCOL_BITMASK protocols;
    NDPI_BITMASK_SET_ALL(protocols);
    ndpi_set_protocol_detection_bitmask2(ndpi, &protocols);

    jobject plist = (*env)->NewObject(env, arrayListClass, arrayListNew);
    if((plist == NULL) || jniCheckException(env))
        return NULL;

    bool success = true;
    int num_protos = (int) ndpi_get_ndpi_num_supported_protocols(ndpi);
    ndpi_proto_defaults_t* proto_defaults = ndpi_get_proto_defaults(ndpi);

    ndpi_protocol_bitmask_struct_t unique_protos;
    NDPI_BITMASK_RESET(unique_protos);

    // NOTE: this does not currently exist as a protocol (see pd_get_proto_name)
    if(!arraylist_add_string(env, arrayListAdd, plist, "HTTPS")) {
        success = false;
        goto out;
    }

    for(int i=0; i<num_protos; i++) {
        ndpi_protocol n_proto = {proto_defaults[i].protoId, NDPI_PROTOCOL_UNKNOWN, NDPI_PROTOCOL_CATEGORY_UNSPECIFIED};
        uint16_t proto = pd_ndpi2proto(n_proto);
        //log_d("protos: %d -> %d -> %d", i, proto_defaults[i].protoId, proto);

        if(!NDPI_ISSET(&unique_protos, proto)) {
            NDPI_SET(&unique_protos, proto);
            const char *name = ndpi_get_proto_name(ndpi, proto);
            //log_d("proto: %d %s", proto, name);

            if(!arraylist_add_string(env, arrayListAdd, plist, name)) {
                success = false;
                goto out;
            }
        }
    }

out:
    if(!success) {
        (*env)->DeleteLocalRef(env, plist);
        plist = NULL;
    }
    ndpi_exit_detection_module(ndpi);

    return(plist);
}
/*
JNIEXPORT void JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_dumpMasterSecret(JNIEnv *env, jclass clazz,
                                                                   jbyteArray secret) {
    jsize sec_len = (*env)->GetArrayLength(env, secret);
    jbyte* sec_data = (*env)->GetByteArrayElements(env, secret, 0);

    if(global_pd && global_pd->pcap_dump.dumper)
        pcap_dump_secret(global_pd->pcap_dump.dumper, sec_data, sec_len);

    (*env)->ReleaseByteArrayElements(env, secret, sec_data, 0);
}*/

/* ******************************************************* */
/*
JNIEXPORT jboolean JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_hasSeenDumpExtensions(JNIEnv *env,
                                                                        jclass clazz) {
    return has_seen_dump_extensions;
}
*/
/* ******************************************************* */
/*
JNIEXPORT jboolean JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_extractKeylogFromPcapng(JNIEnv *env, jclass clazz,
                    jstring pcapng_path, jstring out_path
) {
    const char *pcapng_s = (*env)->GetStringUTFChars(env, pcapng_path, 0);
    const char *out_s = (*env)->GetStringUTFChars(env, out_path, 0);

    bool rv = pcapng_to_keylog(pcapng_s, out_s);

    (*env)->ReleaseStringUTFChars(env, out_path, out_s);
    (*env)->ReleaseStringUTFChars(env, pcapng_path, pcapng_s);
    return rv;
}
*/
char* getStringPref(pcapdroid_t *pd, const char *key, char *buf, int bufsize) {
    JNIEnv *env = pd->env;

    jmethodID midMethod = jniGetMethodID(env, cls.vpn_service, key, "()Ljava/lang/String;");
    jstring obj = (*env)->CallObjectMethod(env, pd->capture_service, midMethod);
    char *rv = NULL;

    if(!jniCheckException(env)) {
        // Null string
        if(obj == NULL)
            return NULL;

        const char *value = (*env)->GetStringUTFChars(env, obj, 0);
        log_to_file("getStringPref(%s) = %s", key, value);

        strncpy(buf, value, bufsize);
        buf[bufsize - 1] = '\0';
        rv = buf;

        (*env)->ReleaseStringUTFChars(env, obj, value);
    }

    (*env)->DeleteLocalRef(env, obj);

    return(rv);
}
u_int32_t getIPv4Pref(JNIEnv *env, jobject vpn_inst, const char *key) {
    struct in_addr addr = {0};

    jmethodID midMethod = jniGetMethodID(env, cls.vpn_service, key, "()Ljava/lang/String;");
    jstring obj = (*env)->CallObjectMethod(env, vpn_inst, midMethod);

    if(!jniCheckException(env)) {
        const char *value = (*env)->GetStringUTFChars(env, obj, 0);
        log_to_file("getIPv4Pref(%s) = %s", key, value);

        if(*value && (inet_aton(value, &addr) == 0))
            log_to_file("%s() returned invalid IPv4 address: %s", key, value);

        (*env)->ReleaseStringUTFChars(env, obj, value);
    }

    (*env)->DeleteLocalRef(env, obj);

    return(addr.s_addr);
}

/* ******************************************************* */

zdtun_ip_t getIPPref(JNIEnv *env, jobject vpn_inst, const char *key, int *ip_ver) {
    zdtun_ip_t rv = {};

    jmethodID midMethod = jniGetMethodID(env, cls.vpn_service, key, "()Ljava/lang/String;");
    jstring obj = (*env)->CallObjectMethod(env, vpn_inst, midMethod);

    if(!jniCheckException(env)) {
        const char *value = (*env)->GetStringUTFChars(env, obj, 0);
        log_to_file("getIPPref(%s) = %s", key, value);

        if(*value) {
            *ip_ver = zdtun_parse_ip(value, &rv);

            if(*ip_ver < 0)
                log_to_file("%s() returned invalid IP address: %s", key, value);
        }

        (*env)->ReleaseStringUTFChars(env, obj, value);
    }

    (*env)->DeleteLocalRef(env, obj);
    return(rv);
}

/* ******************************************************* */

struct in6_addr getIPv6Pref(JNIEnv *env, jobject vpn_inst, const char *key) {
    struct in6_addr addr = {0};

    jmethodID midMethod = jniGetMethodID(env, cls.vpn_service, key, "()Ljava/lang/String;");
    jstring obj = (*env)->CallObjectMethod(env, vpn_inst, midMethod);

    if(!jniCheckException(env)) {
        const char *value = (*env)->GetStringUTFChars(env, obj, 0);
        log_to_file("getIPv6Pref(%s) = %s", key, value);

        if(inet_pton(AF_INET6, value, &addr) != 1)
            log_to_file("%s() returned invalid IPv6 address", key);

        (*env)->ReleaseStringUTFChars(env, obj, value);
    }

    (*env)->DeleteLocalRef(env, obj);

    return(addr);
}
int getIntPref(JNIEnv *env, jobject vpn_inst, const char *key) {
    jint value;
    jmethodID midMethod = jniGetMethodID(env, cls.vpn_service, key, "()I");

    value = (*env)->CallIntMethod(env, vpn_inst, midMethod);
    jniCheckException(env);

    log_to_file("getIntPref(%s) = %d", key, value);

    return(value);
}

static void getLibprogPath(pcapdroid_t *pd, const char *prog_name, char *buf, int bufsize) {
    JNIEnv *env = pd->env;
    jobject prog_str = (*env)->NewStringUTF(env, prog_name);

    buf[0] = '\0';

    if((prog_str == NULL) || jniCheckException(env)) {
        log_to_file("could not allocate get_libprog_path string");
        return;
    }

    jstring obj = (*env)->CallObjectMethod(env, pd->capture_service, mids.getLibprogPath, prog_str);
    (*env)->DeleteLocalRef(env, prog_str);

    if(!jniCheckException(env)) {
        const char *value = (*env)->GetStringUTFChars(env, obj, 0);

        strncpy(buf, value, bufsize);
        buf[bufsize - 1] = '\0';

        (*env)->ReleaseStringUTFChars(env, obj, value);
    }

    (*env)->DeleteLocalRef(env, obj);
}

const char* pd_get_proto_name(pcapdroid_t *pd, uint16_t proto, uint16_t alpn, int ipproto) {
    if(proto == NDPI_PROTOCOL_UNKNOWN) {
        // Return the L3 protocol
        return zdtun_proto2str(ipproto);
    }

    if(proto == NDPI_PROTOCOL_TLS) {
        switch (alpn) {
            case NDPI_PROTOCOL_HTTP:
                return "HTTPS";
            case NDPI_PROTOCOL_MAIL_IMAP:
                return "IMAPS";
            case NDPI_PROTOCOL_MAIL_SMTP:
                return "SMTPS";
            default:
                // go on
                break;
        }
    }

    return ndpi_get_proto_name(pd->ndpi, proto);
}
/* ******************************************************* */

static void notifyServiceStatus(pcapdroid_t *pd, const char *status) {
    JNIEnv *env = pd->env;
    jstring status_str;

    status_str = (*env)->NewStringUTF(env, status);
    //log_to_file("trying to status%s %s",status,status_str);
    (*env)->CallVoidMethod(env, pd->capture_service, mids.sendServiceStatus, status_str);
    if(jniCheckException(env)){
        log_to_file("err to status");
    }else{
        //log_to_file("suc to status");
    }

    (*env)->DeleteLocalRef(env, status_str);
}
static void load_dns_servers(pcapdroid_t *pd) {
    // IP addresses (both legacy and private DNS). These are used to count DNS queries and
    // redirect DNS queries to the public DNS server (see check_dns_req_allowed)
    blacklist_add_ipstr(pd->vpn.known_dns_servers, "8.8.8.8");
    blacklist_add_ipstr(pd->vpn.known_dns_servers, "8.8.4.4");
    blacklist_add_ipstr(pd->vpn.known_dns_servers, "1.1.1.1");
    blacklist_add_ipstr(pd->vpn.known_dns_servers, "1.0.0.1");
    blacklist_add_ipstr(pd->vpn.known_dns_servers, "2001:4860:4860::8888");
    blacklist_add_ipstr(pd->vpn.known_dns_servers, "2001:4860:4860::8844");
    blacklist_add_ipstr(pd->vpn.known_dns_servers, "2606:4700:4700::64");
    blacklist_add_ipstr(pd->vpn.known_dns_servers, "2606:4700:4700::6400");
    blacklist_add_ipstr(pd->vpn.known_dns_servers, "2606:4700:4700::1111");
    blacklist_add_ipstr(pd->vpn.known_dns_servers, "2606:4700:4700::1001");

    // Domains (only private DNS)
    // https://help.firewalla.com/hc/en-us/articles/360060661873-Dealing-DNS-over-HTTPS-and-DNS-over-TLS-on-your-network
    blacklist_add_domain(pd->vpn.known_dns_servers, "dns.google");
    blacklist_add_domain(pd->vpn.known_dns_servers, "chrome.cloudflare-dns.com");
    blacklist_add_domain(pd->vpn.known_dns_servers, "mozilla.cloudflare-dns.com");
    blacklist_add_domain(pd->vpn.known_dns_servers, "doh.cleanbrowsing.org");
    blacklist_add_domain(pd->vpn.known_dns_servers, "chromium.dns.nextdns.io");
    blacklist_add_domain(pd->vpn.known_dns_servers, "firefox.dns.nextdns.io");
    blacklist_add_domain(pd->vpn.known_dns_servers, "dns.quad9.net");
    blacklist_add_domain(pd->vpn.known_dns_servers, "doh.opendns.com");
    blacklist_add_domain(pd->vpn.known_dns_servers, "dns.adguard.com");
    blacklist_add_domain(pd->vpn.known_dns_servers, "dot.libredns.gr");
    blacklist_add_domain(pd->vpn.known_dns_servers, "dns.dnslify.com");
    blacklist_add_domain(pd->vpn.known_dns_servers, "dns-tls.qis.io");
}
static void conns_clear(pcapdroid_t *pd, conn_array_t *arr, bool free_all) {
    if(arr->items) {
        for(int i=0; i < arr->cur_items; i++) {
            conn_and_tuple_t *slot = &arr->items[i];

            if(slot->data && (slot->data->to_purge || free_all))
                pd_purge_connection(pd, slot->data);
        }

        pd_free(arr->items);
        arr->items = NULL;
    }

    arr->size = 0;
    arr->cur_items = 0;
}

static void sendStatsDump(pcapdroid_t *pd) {
    JNIEnv *env = pd->env;
    const capture_stats_t *capstats = &pd->capture_stats;
    const zdtun_statistics_t *stats = &pd->stats;
    jstring allocs_summary =
#ifdef PCAPDROID_TRACK_ALLOCS
    (*pd->env)->NewStringUTF(pd->env, get_allocs_summary());
#else
    NULL;
#endif

    int active_conns = (int)(stats->num_icmp_conn + stats->num_tcp_conn + stats->num_udp_conn);
    int tot_conns = (int)(stats->num_icmp_opened + stats->num_tcp_opened + stats->num_udp_opened);

    jobject stats_obj = (*env)->NewObject(env, cls.stats, mids.statsInit);

    if((stats_obj == NULL) || jniCheckException(env)) {
        log_e("NewObject(CaptureStats) failed");
        return;
    }
    //log_to_file("sending");
    (*env)->CallVoidMethod(env, stats_obj, mids.statsSetData,
                           allocs_summary,
                           capstats->sent_bytes, capstats->rcvd_bytes,
                           capstats->ipv6_sent_bytes, capstats->ipv6_rcvd_bytes,
                           //(jlong)(pd->pcap_dump.dumper ? pcap_get_dump_size(pd->pcap_dump.dumper) : 0),
                           (jlong)( 0),
                           capstats->sent_pkts, capstats->rcvd_pkts,
                           min(pd->num_dropped_pkts, INT_MAX), pd->num_dropped_connections,
                           stats->num_open_sockets, stats->all_max_fd, active_conns, tot_conns,
                           pd->num_dns_requests);

    if(!jniCheckException(env)) {
        (*env)->CallVoidMethod(env, pd->capture_service, mids.sendStatsDump, stats_obj);
        if(jniCheckException(env)){
            log_to_file("excep stat 2");
        }else{
            //log_to_file("suc send");
        }
        //log_to_file("suc send...");
        
    }else{
        log_to_file("excep stat");
    }
    //log_to_file("sended");
    (*env)->DeleteLocalRef(env, allocs_summary);
    (*env)->DeleteLocalRef(env, stats_obj);
}
const char* get_file_path(pcapdroid_t *pd, const char *subpath) {
    strncpy(pd->filesdir + pd->filesdir_len, subpath,
            sizeof(pd->filesdir) - pd->filesdir_len - 1);
    pd->filesdir[sizeof(pd->filesdir) - 1] = 0;
    return pd->filesdir;
}

// called after load_new_blacklists
static void use_new_blacklists(pcapdroid_t *pd) {
    if(!pd->malware_detection.new_bl)
        return;

    if(pd->malware_detection.bl)
        blacklist_destroy(pd->malware_detection.bl);
    pd->malware_detection.bl = pd->malware_detection.new_bl;
    pd->malware_detection.new_bl = NULL;

    bl_status_arr_t *status_arr = pd->malware_detection.status_arr;
    pd->malware_detection.status_arr = NULL;

    if(status_arr == NULL) {
        // NOTE: must notify even if status_arr is NULL
        status_arr = pd_calloc(0, sizeof(bl_status_arr_t));

        if(!status_arr) // this should never happen
            return;
    }

    if(pd->cb.notify_blacklists_loaded)
        pd->cb.notify_blacklists_loaded(pd, status_arr);

    for(int i = 0; i < status_arr->cur_items; i++) {
        bl_status_t *st = &status_arr->items[i];
        pd_free(st->fname);
    }
    pd_free(status_arr->items);
    pd_free(status_arr);
}

/* ******************************************************* */

// Loads the blacklists data into new_bl and sets reload_done.
// use_new_blacklists needs to be called to use it.
static void* load_new_blacklists(void *data) {
    pcapdroid_t *pd = (pcapdroid_t*) data;
    bl_status_arr_t *status_arr = pd_calloc(1, sizeof(bl_status_arr_t));
    if(!status_arr) {
        pd->malware_detection.reload_done = true;
        return NULL;
    }

    blacklist_t *bl = blacklist_init();
    if(!bl) {
        pd_free(status_arr);
        pd->malware_detection.reload_done = true;
        return NULL;
    }

    clock_t start = clock();

    // load files in the malware_bl directory
    for(int i = 0; i < pd->malware_detection.num_bls; i++) {
        bl_info_t *blinfo = &pd->malware_detection.bls_info[i];
        char subpath[256];
        blacklist_stats_t stats;

        snprintf(subpath, sizeof(subpath), "malware_bl/%s", blinfo->fname);

        if(blacklist_load_file(bl, get_file_path(pd, subpath), blinfo->type, &stats) == 0) {
            // NOTE: cannot invoke JNI from this thread, must use an intermediate storage
            if(status_arr->size >= status_arr->cur_items) {
                /* Extend array */
                status_arr->size = (status_arr->size == 0) ? 8 : (status_arr->size * 2);
                status_arr->items = pd_realloc(status_arr->items, status_arr->size * sizeof(bl_status_t));
                if(!status_arr->items) {
                    log_e("realloc(bl_status_arr_t) (%d items) failed", status_arr->size);
                    status_arr->size = 0;
                    continue;
                }
            }

            char *fname = pd_strdup(blinfo->fname);
            if(!fname)
                continue;

            bl_status_t *status = &status_arr->items[status_arr->cur_items++];
            status->fname = fname;
            status->num_rules = stats.num_rules;
        }
    }

    // Test domain/IP to test blacklist match
    blacklist_add_domain(bl, "internetbadguys.com");
    blacklist_add_ipstr(bl, "0.0.0.1");

    log_d("Blacklists loaded in %.3f sec", ((double) (clock() - start)) / CLOCKS_PER_SEC);

    pd->malware_detection.new_bl = bl;
    pd->malware_detection.status_arr = status_arr;
    pd->malware_detection.reload_done = true;
    return NULL;
}

/* ******************************************************* */

struct iter_conn_data {
    pcapdroid_t *pd;
    conn_cb cb;
};

static int zdtun_iter_adapter(zdtun_t *zdt, const zdtun_conn_t *conn_info, void *data) {
    struct iter_conn_data *idata = (struct iter_conn_data*) data;
    const zdtun_5tuple_t *tuple = zdtun_conn_get_5tuple(conn_info);
    pd_conn_t *conn = zdtun_conn_get_userdata(conn_info);

    return idata->cb(idata->pd, tuple, conn);
}

static void iter_active_connections(pcapdroid_t *pd, conn_cb cb) {
    if(!pd->vpn_capture){}
       // pcap_iter_connections(pd, cb);
    else {
        struct iter_conn_data idata = {
                .pd = pd,
                .cb = cb,
        };
        zdtun_iter_connections(pd->zdt, zdtun_iter_adapter, &idata);
    }
}

/* ******************************************************* */

static int check_blocked_conn_cb(pcapdroid_t *pd, const zdtun_5tuple_t *tuple, pd_conn_t *data) {
    zdtun_ip_t dst_ip = tuple->dst_ip;
    blacklist_t *fw_bl = pd->firewall.bl;
    bool old_block = data->to_block;

    data->to_block = (!data->blacklisted_internal || !data->blacklisted_ip || !data->blacklisted_domain);
    if(!data->to_block && pd->firewall.enabled && fw_bl) {
        data->to_block = blacklist_match_uid(fw_bl, data->uid) ||
                         blacklist_match_ip(fw_bl, &dst_ip, tuple->ipver) ||
                         (data->info && data->info[0] && blacklist_match_domain(fw_bl, data->info));
    }

    check_whitelist_mode_block(pd, tuple, data);

    if(old_block != data->to_block) {
        data->update_type |= CONN_UPDATE_STATS;
        pd_notify_connection_update(pd, tuple, data);
    }

    // continue
    return 0;
}

/* Perform a full dump of the active connections */
static void sendConnectionsDump(pcapdroid_t *pd) {
    JNIEnv *env = pd->env;
    //jniDumpReferences(env);

    jobject new_conns = (*env)->NewObjectArray(env, pd->new_conns.cur_items, cls.conn, NULL);
    jobject conns_updates = (*env)->NewObjectArray(env, pd->conns_updates.cur_items, cls.conn_update, NULL);

    if((new_conns == NULL) || (conns_updates == NULL) || jniCheckException(env)) {
        log_e("NewObjectArray() failed");
        goto cleanup;
    }

    // New connections
    for(int i=0; i < pd->new_conns.cur_items; i++) {
        conn_and_tuple_t *conn = &pd->new_conns.items[i];
        conn->data->pending_notification = false;

        if(dumpNewConnection(pd, conn, new_conns, i) < 0)
            goto cleanup;
    }

    //clock_t start = clock();

    // Updated connections
    for(int i=0; i < pd->conns_updates.cur_items; i++) {
        conn_and_tuple_t *conn = &pd->conns_updates.items[i];
        conn->data->pending_notification = false;

        if(dumpConnectionUpdate(pd, conn, conns_updates, i) < 0)
            goto cleanup;
    }

    //double cpu_time_used = ((double) (clock() - start)) / CLOCKS_PER_SEC;
    //log_d("avg cpu_time_used per update: %f sec", cpu_time_used / pd->conns_updates.cur_items);

    /* Send the dump */
    (*env)->CallVoidMethod(env, pd->capture_service, mids.updateConnections, new_conns, conns_updates);
    jniCheckException(env);

cleanup:
    (*env)->DeleteLocalRef(env, new_conns);
    (*env)->DeleteLocalRef(env, conns_updates);
    //jniDumpReferences(env);
}

/* ******************************************************* */

// Load information about the blacklists to use (into pd->malware_detection.bls_info)
static int loadBlacklistsInfo(pcapdroid_t *pd) {
    int rv = 0;
    JNIEnv *env = pd->env;
    jobjectArray *arr = (*env)->CallObjectMethod(env, pd->capture_service, mids.getBlacklistsInfo);
    pd->malware_detection.bls_info = NULL;
    pd->malware_detection.num_bls = 0;

    if((jniCheckException(pd->env) != 0) || (arr == NULL))
        return -1;

    pd->malware_detection.num_bls = (*env)->GetArrayLength(env, arr);
    if(pd->malware_detection.num_bls == 0)
        goto cleanup;

    pd->malware_detection.bls_info = (bl_info_t*) pd_calloc(pd->malware_detection.num_bls, sizeof(bl_info_t));
    if(pd->malware_detection.bls_info == NULL) {
        pd->malware_detection.num_bls = 0;
        rv = -1;
        goto cleanup;
    }

    for(int i = 0; i < pd->malware_detection.num_bls; i++) {
        jobject *bl_descr = (*env)->GetObjectArrayElement(env, arr, i);
        if(bl_descr != NULL) {
            bl_info_t *blinfo = &pd->malware_detection.bls_info[i];

            jstring fname_obj = (*env)->GetObjectField(env, bl_descr, fields.bldescr_fname);
            const char *fname = (*env)->GetStringUTFChars(env, fname_obj, 0);
            blinfo->fname = pd_strdup(fname);
            (*env)->ReleaseStringUTFChars(env, fname_obj, fname);
            (*pd->env)->DeleteLocalRef(pd->env, fname_obj);

            jobject bl_type = (*env)->GetObjectField(env, bl_descr, fields.bldescr_type);
            blinfo->type = (*env)->IsSameObject(env, bl_type, enums.bltype_ip) ? IP_BLACKLIST : DOMAIN_BLACKLIST;
            (*pd->env)->DeleteLocalRef(pd->env, bl_type);

            //log_d("[+] Blacklist: %s (%s)", blinfo->fname, (blinfo->type == IP_BLACKLIST) ? "IP" : "domain");
            (*pd->env)->DeleteLocalRef(pd->env, bl_descr);
        }
    }

cleanup:
    (*pd->env)->DeleteLocalRef(pd->env, arr);
    return rv;
}
/* ******************************************************* */

// Check if a previously blacklisted connection is now whitelisted
static int check_blacklisted_conn_cb(pcapdroid_t *pd, const zdtun_5tuple_t *tuple, pd_conn_t *data) {
    blacklist_t *whitelist = pd->malware_detection.whitelist;
    bool changed = false;

    data->whitelisted_app = blacklist_match_uid(whitelist, data->uid);

    if(data->blacklisted_ip) {
        const zdtun_ip_t dst_ip = tuple->dst_ip;
        if(data->whitelisted_app || blacklist_match_ip(whitelist, &dst_ip, tuple->ipver)) {
            data->blacklisted_ip = false;
            changed = true;
        }
    }

    if(data->blacklisted_domain &&
            (data->whitelisted_app || blacklist_match_domain(whitelist, data->info))) {
        data->blacklisted_domain = false;
        changed = true;
    }

    if(changed) {
        // Possibly unblock the connection
        if(pd->firewall.bl)
            check_blocked_conn_cb(pd, tuple, data);

        data->update_type |= CONN_UPDATE_STATS;
        pd_notify_connection_update(pd, tuple, data);
    }

    // continue
    return 0;
}

void pd_housekeeping(pcapdroid_t *pd) {
    if(dump_capture_stats_now ||
            (pd->capture_stats.new_stats && ((pd->now_ms - pd->capture_stats.last_update_ms) >= CAPTURE_STATS_UPDATE_FREQUENCY_MS))) {
        dump_capture_stats_now = false;
        //log_d("Send stats");

        if(pd->vpn_capture)
            zdtun_get_stats(pd->zdt, &pd->stats);

        if(pd->cb.send_stats_dump)
            pd->cb.send_stats_dump(pd);

        pd->capture_stats.new_stats = false;
        pd->capture_stats.last_update_ms = pd->now_ms;
    } else if (pd->now_ms >= next_connections_dump) {
        /*log_d("sendConnectionsDump [after %" PRIu64 " ms]: new=%d, updates=%d",
              pd->now_ms - last_connections_dump,
              pd->new_conns.cur_items, pd->conns_updates.cur_items);*/

        if ((pd->new_conns.cur_items != 0) || (pd->conns_updates.cur_items != 0)) {
            if (pd->cb.send_connections_dump)
                pd->cb.send_connections_dump(pd);
            conns_clear(pd, &pd->new_conns, false);
            conns_clear(pd, &pd->conns_updates, false);
        }

        last_connections_dump = pd->now_ms;
        next_connections_dump = pd->now_ms + CONNECTION_DUMP_UPDATE_FREQUENCY_MS;
        netd_resolve_waiting = 0;
    } //else if(pd->pcap_dump.dumper && pcap_check_export(pd->pcap_dump.dumper))
        //;
    else if(pd->malware_detection.enabled) {
        // Malware detection
        if(pd->malware_detection.reload_in_progress) {
            if(pd->malware_detection.reload_done) {
                pthread_join(pd->malware_detection.reload_worker, NULL);
                pd->malware_detection.reload_in_progress = false;
                use_new_blacklists(pd);
            }
        } else if(reload_blacklists_now) {
            reload_blacklists_now = false;
            pd->malware_detection.reload_done = false;
            pd->malware_detection.new_bl = NULL;
            pd->malware_detection.status_arr = NULL;
            pthread_create(&pd->malware_detection.reload_worker, NULL, load_new_blacklists,
                           pd);
            pd->malware_detection.reload_in_progress = true;
        }
    }

    if(pd->malware_detection.new_wl) {
        // Load new whitelist
        if(pd->malware_detection.whitelist)
            blacklist_destroy(pd->malware_detection.whitelist);
        pd->malware_detection.whitelist = pd->malware_detection.new_wl;
        pd->malware_detection.new_wl = NULL;

        // Check the active (blacklisted) connections to possibly whitelist (and unblock) them
         iter_active_connections(pd, check_blacklisted_conn_cb);
    }

    if(pd->firewall.new_bl) {
        // Load new blocklist
        if(pd->firewall.bl)
            blacklist_destroy(pd->firewall.bl);
        pd->firewall.bl = pd->firewall.new_bl;
        pd->firewall.new_bl = NULL;
        iter_active_connections(pd, check_blocked_conn_cb);
    } else if(pd->firewall.new_wl) {
        // Load new whitelist
        if(pd->firewall.wl)
            blacklist_destroy(pd->firewall.wl);
        pd->firewall.wl = pd->firewall.new_wl;
        pd->firewall.new_wl = NULL;
        iter_active_connections(pd, check_blocked_conn_cb);
    }

    if(pd->tls_decryption.new_list) {
        // Load new whitelist
        if(pd->tls_decryption.list)
            blacklist_destroy(pd->tls_decryption.list);
        pd->tls_decryption.list = pd->tls_decryption.new_list;
        pd->tls_decryption.new_list = NULL;
    }
}


static bool dumpPayloadChunk(struct pcapdroid *pd, const pkt_context_t *pctx, const char *dump_data, int dump_size) {
    JNIEnv *env = pd->env;
    bool rv = false;

    if(pctx->data->payload_chunks == NULL) {
        // Directly allocating an ArrayList<bytes> rather than creating it afterwards saves us from a data copy.
        // However, this creates a local reference, which is retained until sendConnectionsDump is called.
        // NOTE: Android only allows up to 512 local references.
        pctx->data->payload_chunks = (*env)->NewObject(env, cls.arraylist, mids.arraylistNew);
        if((pctx->data->payload_chunks == NULL) || jniCheckException(env))
            return false;
    }

    jbyteArray barray = (*env)->NewByteArray(env, dump_size);
    if(jniCheckException(env))
        return false;

    jobject chunk_type = (pctx->data->l7proto == NDPI_PROTOCOL_HTTP) ? enums.chunktype_http : enums.chunktype_raw;

    jobject chunk = (*env)->NewObject(env, cls.payload_chunk, mids.payloadChunkInit, barray, chunk_type, pctx->is_tx, pctx->ms);
    if(chunk && !jniCheckException(env)) {
        (*env)->SetByteArrayRegion(env, barray, 0, dump_size, (jbyte*) dump_data);
        //rv = (*env)->CallBooleanMethod(env, pctx->data->payload_chunks, mids.arraylistAdd, chunk);
    }

    log_d("Dump chunk [size=%d]: %d", rv, dump_size);

    (*env)->DeleteLocalRef(env, barray);
    (*env)->DeleteLocalRef(env, chunk);
    return rv;
}

/* ******************************************************* */

static void clearPayloadChunks(struct pcapdroid *pd, const pkt_context_t *pctx) {
    JNIEnv *env = pd->env;

    if (pctx->data->payload_chunks) {
        (*env)->DeleteLocalRef(env, pctx->data->payload_chunks);
        pctx->data->payload_chunks = NULL;
    }
}
static void sendPcapDump(struct pcapdroid *pd, const int8_t *buf, int dump_size) {
    JNIEnv *env = pd->env;

    //log_d("Exporting a %d B PCAP buffer", pd->pcap_dump.buffer_idx);

    jbyteArray barray = (*env)->NewByteArray(env, dump_size);
    if(jniCheckException(env))
        return;

    (*env)->SetByteArrayRegion(env, barray, 0, dump_size, buf);
    (*env)->CallVoidMethod(env, pd->capture_service, mids.dumpPcapData, barray);
    jniCheckException(env);

    (*env)->DeleteLocalRef(env, barray);
}

/* ******************************************************* */

static void stopPcapDump(pcapdroid_t *pd) {
    JNIEnv *env = pd->env;

    (*env)->CallVoidMethod(env, pd->capture_service, mids.stopPcapDump);
    jniCheckException(env);
}

static bool check_dns_req_allowed(pcapdroid_t *pd, zdtun_conn_t *conn, pkt_context_t *pctx) {
    const zdtun_5tuple_t *tuple = pctx->tuple;

    if(new_dns_server != 0) {
        log_i("Using new DNS server");
        pd->vpn.ipv4.dns_server = new_dns_server;
        new_dns_server = 0;
    }

    if(pctx->tuple->ipproto == IPPROTO_ICMP)
        return true;

    bool is_internal_dns = pd->vpn.ipv4.enabled && (tuple->ipver == 4) && (tuple->dst_ip.ip4 == pd->vpn.ipv4.internal_dns);
    bool is_dns_server = is_internal_dns
                         || (pd->vpn.ipv6.enabled && (tuple->ipver == 6) && (memcmp(&tuple->dst_ip.ip6, &pd->vpn.ipv6.dns_server, 16) == 0));

    if(!is_dns_server) {
        // try with known DNS servers
        zdtun_ip_t dst_ip = tuple->dst_ip;

        if(blacklist_match_ip(pd->vpn.known_dns_servers, &dst_ip, tuple->ipver)) {
            char ip[INET6_ADDRSTRLEN];
            int family = (tuple->ipver == 4) ? AF_INET : AF_INET6;

            is_dns_server = true;
            ip[0] = '\0';
            inet_ntop(family, &dst_ip, (char *)&ip, sizeof(ip));

            log_d("Matched known DNS server: %s", ip);
        }
    }

    if(!is_dns_server)
        return(true);

    if((tuple->ipproto == IPPROTO_UDP) && (ntohs(tuple->dst_port) == 53)) {
        zdtun_pkt_t *pkt = pctx->pkt;
        int dns_length = pkt->l7_len;

        if(dns_length >= sizeof(dns_packet_t)) {
            dns_packet_t *dns_data = (dns_packet_t*) pkt->l7;

            if((dns_data->flags & DNS_FLAGS_MASK) != DNS_TYPE_REQUEST)
                return(true);

            pd->num_dns_requests++;

            if(is_internal_dns) {
                /*
                 * Direct the packet to the public DNS server. Checksum recalculation is not strictly necessary
                 * here as zdtun will pd the connection.
                 */
                zdtun_ip_t ip = {0};
                ip.ip4 = pd->vpn.ipv4.dns_server;
                zdtun_conn_dnat(conn, &ip, htons(53), 4);
            }

            return(true);
        }
    }

    if(block_private_dns) {
        log_d("blocking packet directed to the DNS server");
        return(false);
    }

    // allow
    return(true);
}

/* ******************************************************* */

static bool spoof_dns_reply(pcapdroid_t *pd, zdtun_conn_t *conn, pkt_context_t *pctx) {
    // Step 1: ensure that this is a valid query
    zdtun_pkt_t *pkt = pctx->pkt;
    if(pkt->l7_len < (sizeof(dns_packet_t) + 5))
        return false;

    dns_packet_t *req = (dns_packet_t*) pkt->l7;
    if(ntohs(req->questions) != 1)
        return false;

    int remaining = pkt->l7_len - sizeof(dns_packet_t);
    int qlen=0;
    while(remaining >= 5) {
        if(!req->queries[qlen])
            break;
        qlen++;
        remaining--;
    }

    if((req->queries[qlen] != 0) || (req->queries[qlen + 1] != 0) ||
       (req->queries[qlen + 3] != 0) || (req->queries[qlen + 4] != 1))
        return false; // invalid

    uint8_t qtype = req->queries[qlen + 2];
    if((qtype != 0x01) && (qtype != 0x1c))
        return false; // invalid query type

    // Step 2: spoof the reply
    log_d("Spoofing %s DNS reply", (qtype == 0x01) ? "A" : "AAAA");

    const zdtun_5tuple_t *tuple = pctx->tuple;
    uint8_t alen = (qtype == 0x01) ? 4 : 16;
    int iplen = zdtun_iphdr_len(pd->zdt, conn);
    unsigned int len = iplen + 8 /* UDP */ + sizeof(dns_packet_t) + qlen + 5 /* type, ... */ + 12 /* answer */ + alen;
    char buf[len];
    memset(buf, 0, len);

    zdtun_make_iphdr(pd->zdt, conn, buf, len - iplen);

    struct udphdr *udp = (struct udphdr*)(buf + iplen);
    udp->uh_sport = tuple->dst_port;
    udp->uh_dport = tuple->src_port;
    udp->len = htons(len - iplen);

    dns_packet_t *dns = (dns_packet_t*)(buf + iplen + 8);
    dns->transaction_id = req->transaction_id;
    dns->flags = htons(0x8180);
    dns->questions = req->questions;
    dns->answ_rrs = dns->questions;
    dns->auth_rrs = dns->additional_rrs = 0;

    // Queries
    memcpy(dns->queries, req->queries, qlen + 5);

    // Answers
    uint8_t *answ = dns->queries + qlen + 5;

    answ[0] = 0xc0, answ[1] = 0x0c;        // name ptr
    answ[2] = 0x00, answ[3] = qtype;       // type
    answ[4] = 0x00, answ[5] = 0x01;        // class IN
    *(uint32_t*)(answ + 6) = htonl(10); // TTL: 10s
    answ[10] = 0x00, answ[11] = alen;      // addr length
    memset(answ + 12, 0, alen);      // addr: 0.0.0.0/::

    // checksum
    udp->uh_sum = 0;
    udp->uh_sum = zdtun_l3_checksum(pd->zdt, conn, buf, (char*)udp, len - iplen);

    //hexdump(buf, len);
    write(pd->vpn.tunfd, buf, len);

    return true;
}
int run_vpn(pcapdroid_t *pd) {
    zdtun_t *zdt;
    char buffer[VPN_BUFFER_SIZE];
    u_int64_t next_purge_ms;

    int flags = fcntl(pd->vpn.tunfd, F_GETFL, 0);
    if (flags < 0 || fcntl(pd->vpn.tunfd, F_SETFL, flags & ~O_NONBLOCK) < 0) {
        log_f("fcntl ~O_NONBLOCK error [%d]: %s", errno,
                    strerror(errno));
        return (-1);
    }

#if ANDROID
    pd->vpn.resolver = init_uid_resolver(pd->sdk_ver, pd->env, pd->capture_service);
    pd->vpn.known_dns_servers = blacklist_init();
    pd->vpn.block_quic_mode = getIntPref(pd->env, pd->capture_service, "getBlockQuickMode");

    pd->vpn.ipv4.enabled = (bool) getIntPref(pd->env, pd->capture_service, "getIPv4Enabled");
    pd->vpn.ipv4.dns_server = getIPv4Pref(pd->env, pd->capture_service, "getDnsServer");
    pd->vpn.ipv4.internal_dns = getIPv4Pref(pd->env, pd->capture_service, "getVpnDns");

    pd->vpn.ipv6.enabled = (bool) getIntPref(pd->env, pd->capture_service, "getIPv6Enabled");
    pd->vpn.ipv6.dns_server = getIPv6Pref(pd->env, pd->capture_service, "getIpv6DnsServer");
#endif

    zdtun_callbacks_t callbacks = {
        .send_client = remote2vpn,
        .account_packet = update_conn_status,
        .on_socket_open = protectSocketCallback,
        .on_connection_open = handle_new_connection,
        .on_connection_close = connection_closed,
    };

    load_dns_servers(pd);

    zdt = zdtun_init(&callbacks, pd);
    if(zdt == NULL) {
        log_f("zdtun_init failed");
        return(-2);
    }

#if ANDROID
    zdtun_set_mtu(zdt, getIntPref(pd->env, pd->capture_service, "getVpnMTU"));
#endif

    pd->zdt = zdt;
    new_dns_server = 0;

    if(pd->socks5.enabled) {
        zdtun_set_socks5_proxy(zdt, &pd->socks5.proxy_ip, pd->socks5.proxy_port, pd->socks5.proxy_ipver);

        if(pd->socks5.proxy_user[0] && pd->socks5.proxy_pass[0])
            zdtun_set_socks5_userpass(zdt, pd->socks5.proxy_user, pd->socks5.proxy_pass);
    }

    pd_refresh_time(pd);
    next_purge_ms = pd->now_ms + PERIODIC_PURGE_TIMEOUT_MS;

    log_to_file("Starting packet loop");
    if(pd->cb.notify_service_status && running){
        pd->cb.notify_service_status(pd, "started");
        //log_to_file("try noti status started");
    }

    while(running) {
        int max_fd;
        fd_set fdset;
        fd_set wrfds;
        int size;
        struct timeval timeout = {.tv_sec = 0, .tv_usec = SELECT_TIMEOUT_MS * 1000};

        zdtun_fds(zdt, &max_fd, &fdset, &wrfds);

        FD_SET(pd->vpn.tunfd, &fdset);
        max_fd = max(max_fd, pd->vpn.tunfd);

        if((select(max_fd + 1, &fdset, &wrfds, NULL, &timeout) < 0) && (errno != EINTR)) {
            log_to_file("select failed[%d]: %s", errno, strerror(errno));
            break;
        }

        if(!running)
            break;

        if(FD_ISSET(pd->vpn.tunfd, &fdset)) {
            /* Packet from VPN */
            size = read(pd->vpn.tunfd, buffer, sizeof(buffer));
            if(size > 0) {
                zdtun_pkt_t pkt;
                pd_refresh_time(pd);
//log_to_file("1");
                if(zdtun_parse_pkt(zdt, buffer, size, &pkt) != 0) {
                    log_d("zdtun_parse_pkt failed");
                    goto housekeeping;
                }
//log_to_file("2");
                if(pkt.flags & ZDTUN_PKT_IS_FRAGMENT) {
                    log_d("discarding IP fragment");
                    pd->num_discarded_fragments++;
                    goto housekeeping;
                }
//log_to_file("3");
                bool is_internal_dns = pd->vpn.ipv4.enabled && (pkt.tuple.ipver == 4) && (pkt.tuple.dst_ip.ip4 == pd->vpn.ipv4.internal_dns);
                if(is_internal_dns && ntohs(pkt.tuple.dst_port) == 853) {
                    // accepting this packet could result in multiple TCP connections being spammed
                    log_d("discarding private DNS packet directed to internal DNS");
                    goto housekeeping;
                }
//log_to_file("4");
                if(((pkt.tuple.ipver == 6) && !pd->vpn.ipv6.enabled) ||
                        ((pkt.tuple.ipver == 4) && !pd->vpn.ipv4.enabled)) {
                    char buf[512];

                    log_d("ignoring IPv%d packet: %s", pkt.tuple.ipver,
                                zdtun_5tuple2str(&pkt.tuple, buf, sizeof(buf)));
                    goto housekeeping;
                }
//log_to_file("5");
                // Skip established TCP connections
                uint8_t is_tcp_established = ((pkt.tuple.ipproto == IPPROTO_TCP) &&
                                              (!(pkt.tcp->th_flags & TH_SYN) || (pkt.tcp->th_flags & TH_ACK)));

                zdtun_conn_t *conn = zdtun_lookup(zdt, &pkt.tuple, !is_tcp_established);
                if (!conn) {
                    if(!is_tcp_established) {
                        char buf[512];

                        pd->num_dropped_connections++;
                        log_e("zdtun_lookup failed: %s",
                                    zdtun_5tuple2str(&pkt.tuple, buf, sizeof(buf)));
                    } else {
                        char buf[512];

                        log_d("skipping established TCP: %s",
                                    zdtun_5tuple2str(&pkt.tuple, buf, sizeof(buf)));
                    }
                    goto housekeeping;
                }
//log_to_file("6");
                // Process the packet
                struct timeval tv;
                const zdtun_5tuple_t *tuple = zdtun_conn_get_5tuple(conn);
                pkt_context_t pctx;
                pd_conn_t *data = zdtun_conn_get_userdata(conn);
//log_to_file("6");
                // To be run before pd_process_packet/process_payload
                if(data->sent_pkts == 0) {
                    if(pd_check_port_map(conn)){
                        data->port_mapping_applied = true;}
                    else if(should_proxify(pd, tuple, data)) {
                        log_to_file("23");
                        zdtun_conn_proxy(conn);
                        log_to_file("24");
                        data->proxied = true;
                    }
                }
//log_to_file("7");
                pd_init_pkt_context(&pctx, &pkt, true, tuple, data, get_pkt_timestamp(pd, &tv));
                pd_process_packet(pd, &pctx);
                if(data->sent_pkts == 0) {
                    // Newly created connections
                    if (!data->port_mapping_applied)
                        data->blacklisted_internal |= !check_dns_req_allowed(pd, conn, &pctx);
                    data->to_block |= data->blacklisted_internal;

                    if(data->to_block) {
                        // blocking a DNS query can cause multiple requests to be spammed. Better to
                        // spoof a reply with an invalid IP.
                        if((data->l7proto == NDPI_PROTOCOL_DNS) && (tuple->ipproto == IPPROTO_UDP)) {
                            spoof_dns_reply(pd, conn, &pctx);
                            zdtun_conn_close(zdt, conn, CONN_STATUS_CLOSED);
                        }
                    }
                }
//log_to_file("8");
                if(data->to_block) {
                    data->blocked_pkts++;
                    data->update_type |= CONN_UPDATE_STATS;
                    pd_notify_connection_update(pd, tuple, data);
                    goto housekeeping;
                }
//log_to_file("9");
                // NOTE: zdtun_forward will call remote2vpn
                data->vpn.fw_pctx = &pctx;
                if(zdtun_forward(zdt, &pkt, conn) != 0) {
                    char buf[512];
                    zdtun_conn_status_t status = zdtun_conn_get_status(conn);

                    if(status != CONN_STATUS_UNREACHABLE) {
                        log_to_file("zdtun_forward failed[%d]: %s", status,
                              zdtun_5tuple2str(&pkt.tuple, buf, sizeof(buf)));

                        pd->num_dropped_connections++;
                    } else
                        log_to_file("%s: net/host unreachable", zdtun_5tuple2str(&pkt.tuple, buf, sizeof(buf)));

                    zdtun_conn_close(zdt, conn, CONN_STATUS_ERROR);
                    goto housekeeping;
                } else {
                    // zdtun_forward was successful
                    if(data->vpn.fw_pctx) {
                        // it was not accounted in remote2vpn, account here
                        pd_account_stats(pd, data->vpn.fw_pctx);
                        data->vpn.fw_pctx = NULL;
                    }

                    // First forwarded packet
                    if(data->sent_pkts == 1) {
                        // The socket is open only after zdtun_forward is called
                        socket_t sock = zdtun_conn_get_socket(conn);

                        // In SOCKS5 with the MitmReceiver, we need the local port to the SOCKS5 proxy
                        if((sock != INVALID_SOCKET) && (tuple->ipver == 4)) {
                            // NOTE: the zdtun SOCKS5 implementation only supports IPv4 right now.
                            // If it also supported IPv6, than we would need to expose "sock_ipver"
                            struct sockaddr_in local_addr;
                            socklen_t addrlen = sizeof(local_addr);

                            if(getsockname(sock, (struct sockaddr*) &local_addr, &addrlen) == 0)
                                data->vpn.local_port = local_addr.sin_port;
                        }
                    }
                }
            } else {
                pd_refresh_time(pd);
                if(size < 0)
                    log_e("recv(tunfd) returned error [%d]: %s", errno,
                          strerror(errno));
            }
        } else {
            pd_refresh_time(pd);
            zdtun_handle_fd(zdt, &fdset, &wrfds);
        }

        housekeeping:
        pd_housekeeping(pd);

        if(pd->now_ms >= next_purge_ms) {
            zdtun_purge_expired(zdt);
            next_purge_ms = pd->now_ms + PERIODIC_PURGE_TIMEOUT_MS;
        }
    }

    pd_reset_port_map();
    zdtun_finalize(zdt);

#if ANDROID
    destroy_uid_resolver(pd->vpn.resolver);
    blacklist_destroy(pd->vpn.known_dns_servers);
#endif

    return(0);
}



int pd_run(pcapdroid_t *pd) {
    /* Important: init global state every time. Android may reuse the service. */
    running = true;
    has_seen_dump_extensions = false;
    netd_resolve_waiting = 0;

    // nDPI 
    pd->ndpi = init_ndpi();
    if(pd->ndpi == NULL) {
        log_f("nDPI initialization failed");
        return(-1);
    }

    pd->ip_to_host = ip_lru_init(MAX_HOST_LRU_SIZE);

    if(pd->malware_detection.enabled && pd->cb.load_blacklists_info)
        pd->cb.load_blacklists_info(pd);

    // Load the blacklist before starting
    if(pd->malware_detection.enabled && reload_blacklists_now) {
        reload_blacklists_now = false;
        load_new_blacklists(pd);
        use_new_blacklists(pd);
    }



    memset(&pd->stats, 0, sizeof(pd->stats));

    pd_refresh_time(pd);
    last_connections_dump = pd->now_ms;
    next_connections_dump = last_connections_dump + 500 /* first update after 500 ms */;
    bl_num_checked_connections = 0;
    fw_num_checked_connections = 0;

    // Run the capture
   // int rv = pd->vpn_capture ? run_vpn(pd) : run_pcap(pd);
    int rv = pd->vpn_capture ? run_vpn(pd) : run_vpn(pd);
    log_i("Stopped packet loop");

    // send last dump
    if(pd->cb.send_stats_dump)
        pd->cb.send_stats_dump(pd);
    if(pd->cb.send_connections_dump)
        pd->cb.send_connections_dump(pd);

    conns_clear(pd, &pd->new_conns, true);
    conns_clear(pd, &pd->conns_updates, true);

    if(pd->firewall.bl)
        blacklist_destroy(pd->firewall.bl);
    if(pd->firewall.new_bl)
        blacklist_destroy(pd->firewall.new_bl);
    if(pd->firewall.wl)
        blacklist_destroy(pd->firewall.wl);
    if(pd->firewall.new_wl)
        blacklist_destroy(pd->firewall.new_wl);
    if(pd->tls_decryption.list)
        blacklist_destroy(pd->tls_decryption.list);
    if(pd->tls_decryption.new_list)
        blacklist_destroy(pd->tls_decryption.new_list);

    if(pd->malware_detection.enabled) {
        if(pd->malware_detection.reload_in_progress) {
            log_i("Joining blacklists reload_worker");
            pthread_join(pd->malware_detection.reload_worker, NULL);
        }
        if(pd->malware_detection.bl)
            blacklist_destroy(pd->malware_detection.bl);
        if(pd->malware_detection.whitelist)
            blacklist_destroy(pd->malware_detection.whitelist);
        if(pd->malware_detection.new_wl)
            blacklist_destroy(pd->malware_detection.new_wl);
        if(pd->malware_detection.bls_info) {
            for(int i=0; i < pd->malware_detection.num_bls; i++)
                pd_free(pd->malware_detection.bls_info[i].fname);
            pd_free(pd->malware_detection.bls_info);
        }
    }

#ifndef FUZZING
    ndpi_exit_detection_module(pd->ndpi);
#endif

   
    uid_to_app_t *e, *tmp;
    HASH_ITER(hh, pd->uid2app, e, tmp) {
        HASH_DEL(pd->uid2app, e);
        pd_free(e);
    }

    log_i("Host LRU cache size: %d", ip_lru_size(pd->ip_to_host));
    log_i("Discarded fragments: %ld", pd->num_discarded_fragments);
    ip_lru_destroy(pd->ip_to_host);

    return(rv);
}

JNIEXPORT jboolean JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_reloadBlocklist(JNIEnv *env, jclass clazz,
        jobject ld) {
    pcapdroid_t *pd = global_pd;
    if(!pd) {
        log_e("NULL pd instance");
        return false;
    }

    if(!pd->vpn_capture) {
        log_e("firewall in root mode not implemented");
        return false;
    }

    if(pd->firewall.new_bl != NULL) {
        log_e("previous blocklist not loaded yet");
        return false;
    }

    blacklist_t *bl = blacklist_init();
    if(!bl) {
        log_e("blacklist_init failed");
        return false;
    }

    if(blacklist_load_list_descriptor(bl, env, ld) < 0) {
        log_f("Could not load firewall rules. Check the log for more details");
        blacklist_destroy(bl);
        return false;
    }

    blacklists_stats_t stats;
    blacklist_get_stats(bl, &stats);
    log_d("reloadBlocklist: %d apps, %d domains, %d IPs", stats.num_apps, stats.num_domains, stats.num_ips);

    pd->firewall.new_bl = bl;
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_reloadFirewallWhitelist(JNIEnv *env, jclass clazz,
         jobject whitelist) {
    pcapdroid_t *pd = global_pd;
    if(!pd) {
        log_e("NULL pd instance");
        return false;
    }

    if(!pd->vpn_capture) {
        log_e("firewall in root mode not implemented");
        return false;
    }

    if(pd->firewall.new_wl != NULL) {
        log_e("previous firewall whitelist not loaded yet");
        return false;
    }

    if(whitelist == NULL) {
        pd->firewall.wl_enabled = false;
        log_d("firewall whitelist is disabled");
        return true;
    }

    blacklist_t *wl = blacklist_init();
    if(!wl) {
        log_e("blacklist_init failed");
        return false;
    }

    if(blacklist_load_list_descriptor(wl, env, whitelist) < 0) {
        log_f("Could not load firewall whitelist rules. Check the log for more details");
        blacklist_destroy(wl);
        return false;
    }

    blacklists_stats_t stats;
    blacklist_get_stats(wl, &stats);
    log_d("reloadFirewallWhitelist: %d apps, %d domains, %d IPs", stats.num_apps, stats.num_domains, stats.num_ips);

    pd->firewall.new_wl = wl;
    pd->firewall.wl_enabled = true;
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_reloadMalwareWhitelist(JNIEnv *env, jclass clazz,
        jobject whitelist) {
    pcapdroid_t *pd = global_pd;
    if(!pd) {
        log_e("NULL pd instance");
        return false;
    }

    if(!pd->malware_detection.enabled) {
        log_e("malware detection not enabled");
        return false;
    }

    if(pd->malware_detection.new_wl != NULL) {
        log_e("previous malware whitelist not loaded yet");
        return false;
    }

    blacklist_t *wl = blacklist_init();
    if(!wl) {
        log_e("blacklist_init failed");
        return false;
    }

    if(blacklist_load_list_descriptor(wl, env, whitelist) < 0) {
        log_f("Could not load malware whitelist rules. Check the log for more details");
        blacklist_destroy(wl);
        return false;
    }

    blacklists_stats_t stats;
    blacklist_get_stats(wl, &stats);
    log_d("reloadMalwareWhitelist: %d apps, %d domains, %d IPs", stats.num_apps, stats.num_domains, stats.num_ips);

    pd->malware_detection.new_wl = wl;
    return true;
}
JNIEXPORT jboolean JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_reloadDecryptionList(JNIEnv *env,
                                                                       jclass clazz, jobject listobj) {
    pcapdroid_t *pd = global_pd;
    if(!pd) {
        log_e("NULL pd instance");
        return false;
    }

    if(!pd->tls_decryption.enabled) {
        log_e("TLS decryption not enabled");
        return false;
    }

    if(pd->tls_decryption.new_list != NULL) {
        log_e("previous decryption list not loaded yet");
        return false;
    }

    blacklist_t *list = blacklist_init();
    if(!list) {
        log_e("blacklist_init failed");
        return false;
    }

    if(blacklist_load_list_descriptor(list, env, listobj) < 0) {
        log_f("Could not load decryption list. Check the log for more details");
        blacklist_destroy(list);
        return false;
    }

    blacklists_stats_t stats;
    blacklist_get_stats(list, &stats);
    log_d("reloadDecryptionList: %d apps, %d domains, %d IPs", stats.num_apps, stats.num_domains, stats.num_ips);

    pd->tls_decryption.new_list = list;
    return true;
}
JNIEXPORT void JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_nativeSetFirewallEnabled(JNIEnv *env, jclass clazz, jboolean enabled) {
    pcapdroid_t *pd = global_pd;
    if(!pd) {
        log_e("NULL pd instance");
        return;
    }

    pd->firewall.enabled = enabled;
}
// --- לולאה ראשית ופונקציות JNI ---

//JNIEXPORT void JNICALL Java_com_domain_filter_MyVpnService_nativeStart(JNIEnv *env, jobject thiz, jint tun_fd,) { // Function start
JNIEXPORT void JNICALL
Java_com_emanuelef_remote_1capture_CaptureService_runPacketLoop(JNIEnv *env, jclass type, jint tunfd,
                                                              jobject vpn, jint sdk) {

    log_to_file("nativeStart נקרא. TUN FD: %d", tunfd);
   init_jni(env);
    //running = 1;
    running = true;
    //tun_fd_global = tunfd; // שמירת TUN FD עבור פעולות כתיבה
    /*
    // אתחול סשנים
    for (int i = 0; i < MAX_SESSIONS; i++) { // Loop start for sessions initialization
        sessions[i].remote_socket_fd = -1; // סימון כל הסשנים כלא פעילים
        sessions[i].hostname[0] = '\0'; // ודא ששם הדומיין מאופס
        sessions[i].state = TCP_STATE_CLOSED;
        sessions[i].client_mss = 0;
        sessions[i].server_mss = 0;
    } // Loop end for sessions initialization
    srand(time(NULL)); // אתחול מחולל מספרים אקראיים עבור מספרי רצף

    // הגדרת שרתי ה-DNS עבור הפרוקסי
    dns_server_count = 0;
    
    // Google Public DNS (IPv4)
    struct sockaddr_in dns4_addr;
    memset(&dns4_addr, 0, sizeof(dns4_addr));
    dns4_addr.sin_family = AF_INET;
    dns4_addr.sin_port = htons(53);
    inet_pton(AF_INET, "8.8.8.8", &dns4_addr.sin_addr);
    memcpy(&dns_servers[dns_server_count], &dns4_addr, sizeof(dns4_addr));
    dns_server_lens[dns_server_count] = sizeof(dns4_addr);
    dns_server_count++;

    // Google Public DNS (IPv6)
    struct sockaddr_in6 dns6_addr;
    memset(&dns6_addr, 0, sizeof(dns6_addr));
    dns6_addr.sin6_family = AF_INET6;
    dns6_addr.sin6_port = htons(53);
    inet_pton(AF_INET6, "2001:4860:4860::8888", &dns6_addr.sin6_addr);
    memcpy(&dns_servers[dns_server_count], &dns6_addr, sizeof(dns6_addr));
    dns_server_lens[dns_server_count] = sizeof(dns6_addr);
    dns_server_count++;

    log_to_file("Native DNS servers configured: IPv4 %s:%d, IPv6 %s:%d",
                addr_to_string(&dns_servers[0], (char[INET6_ADDRSTRLEN]){0}, INET6_ADDRSTRLEN), ntohs(((struct sockaddr_in*)&dns_servers[0])->sin_port),
                addr_to_string(&dns_servers[1], (char[INET6_ADDRSTRLEN]){0}, INET6_ADDRSTRLEN), ntohs(((struct sockaddr_in6*)&dns_servers[1])->sin6_port));


    // יצירת מערך pollfd עבור התקן TUN וכל הסוקטים המרוחקים הפעילים
    struct pollfd *fds = (struct pollfd*)malloc(sizeof(struct pollfd) * (1 + MAX_SESSIONS));
    if (!fds) { // Malloc failed
        log_to_file("שגיאה: נכשל בהקצאת מערך pollfd.");
        return;
    } // End of malloc failed

    char tun_buf[MAX_PACKET_SIZE]; 
    char internet_buf[MAX_PACKET_SIZE]; 

    while (running) { // Main loop start
        int num_fds = 0;

        // הוספת TUN FD ל-poll
        fds[num_fds].fd = tun_fd;
        fds[num_fds].events = POLLIN;
        num_fds++;

        // הוספת סוקטים מרוחקים פעילים ל-poll
        for (int i = 0; i < MAX_SESSIONS; i++) { // Sessions loop start
            if (sessions[i].remote_socket_fd != -1) { // Check if session is active
                // בדיקת חיבורים שעדיין במצב SYN_SENT
                if (sessions[i].state == TCP_STATE_SYN_SENT) { // SYN_SENT check start
                    int error = 0;
                    socklen_t len = sizeof(error);
                    // שימוש ב-getsockopt עם SO_ERROR כדי לבדוק סטטוס חיבור לא חוסם
                    if (getsockopt(sessions[i].remote_socket_fd, SOL_SOCKET, SO_ERROR, &error, &len) == 0) { // getsockopt success
                        if (error == 0) { // חיבור הצליח
                            char local_ip_str[INET6_ADDRSTRLEN], remote_ip_str[INET6_ADDRSTRLEN];
                            uint16_t local_port, remote_port;
                            if (sessions[i].addr_family == AF_INET) { // Block start for AF_INET
                                local_port = ((const struct sockaddr_in *)&sessions[i].local_addr)->sin_port;
                                remote_port = ((const struct sockaddr_in *)&sessions[i].remote_addr)->sin_port;
                            } else { // AF_INET6
                                const struct sockaddr_in6 *temp_local_in6_ptr = (const struct sockaddr_in6 *)&sessions[i].local_addr;
                                const struct sockaddr_in6 *temp_remote_in6_ptr = (const struct sockaddr_in6 *)&sessions[i].remote_addr;
                                local_port = temp_local_in6_ptr->sin6_port;
                                remote_port = temp_remote_in6_ptr->sin6_port;
                            } // Block end for port extraction

                            log_to_file("TCP: חיבור מוצלח לשרת %s:%d (FD: %d) עבור סשן %s:%d -> %s:%d (דומיין: %s, AF: %d)",
                                        addr_to_string(&sessions[i].remote_addr, remote_ip_str, sizeof(remote_ip_str)), ntohs(remote_port),
                                        sessions[i].remote_socket_fd,
                                        addr_to_string(&sessions[i].local_addr, local_ip_str, sizeof(local_ip_str)), ntohs(local_port),
                                        addr_to_string(&sessions[i].remote_addr, remote_ip_str, sizeof(remote_ip_str)), ntohs(remote_port),
                                        sessions[i].hostname[0] != '\0' ? sessions[i].hostname : "לא ידוע",
                                        sessions[i].addr_family);
                            sessions[i].state = TCP_STATE_SYN_RECEIVED; // השרת מחובר, עכשיו ניתן לשלוח SYN-ACK ללקוח
                            
                            // שליחת SYN-ACK בחזרה ללקוח TUN
                            struct tcp_header synack_tcph;
                            memset(&synack_tcph, 0, sizeof(synack_tcph));
                            synack_tcph.th_sport = remote_port; 
                            synack_tcph.th_dport = local_port; 
                            synack_tcph.th_seq = htonl(sessions[i].server_seq_num); 
                            synack_tcph.th_ack = htonl(sessions[i].client_seq_num); 
                            synack_tcph.th_flags = TH_SYN | TH_ACK; 
                            synack_tcph.th_win = sessions[i].window_size; 

                            uint8_t tcp_options[40]; 
                            int options_len = 0;
                            
                            uint16_t mss_to_announce = VPN_RECOMMENDED_MSS; 
                            options_len += create_mss_option(tcp_options + options_len, mss_to_announce);
                            // th_off is header length in 32-bit words, including options
                            synack_tcph.th_off = (sizeof(struct tcp_header) + options_len + 3) / 4; 
                            synack_tcph.th_off = (synack_tcph.th_off < 5) ? 5 : synack_tcph.th_off; 

                            synack_tcph.th_sum = 0; 
                            if (sessions[i].addr_family == AF_INET) { // Block start for AF_INET
                                synack_tcph.th_sum = transport_checksum_ipv4(
                                    ((const struct sockaddr_in *)&sessions[i].remote_addr)->sin_addr.s_addr,
                                    ((const struct sockaddr_in *)&sessions[i].local_addr)->sin_addr.s_addr,
                                    IPPROTO_TCP, &synack_tcph, sizeof(struct tcp_header) + options_len, NULL, 0);
                            } else { // AF_INET6
                                 struct in6_addr temp_remote_in6_addr_copy = ((const struct sockaddr_in6 *)&sessions[i].remote_addr)->sin6_addr;
                                 struct in6_addr temp_local_in6_addr_copy = ((const struct sockaddr_in6 *)&sessions[i].local_addr)->sin6_addr;
                                 synack_tcph.th_sum = transport_checksum_ipv6(
                                    &temp_remote_in6_addr_copy, 
                                    &temp_local_in6_addr_copy, 
                                    IPPROTO_TCP, sizeof(struct tcp_header) + options_len,
                                    &synack_tcph, sizeof(struct tcp_header) + options_len, NULL, 0);
                            } // Block end for address family

                            send_packet_to_tun(&sessions[i].remote_addr, &sessions[i].local_addr, IPPROTO_TCP,
                                               &synack_tcph, sizeof(struct tcp_header) + options_len, NULL, 0, ++sessions[i].ip_id_counter);
                        } else { // חיבור נכשל (error != 0)
                            char local_ip_str[INET6_ADDRSTRLEN], remote_ip_str[INET6_ADDRSTRLEN];
                            uint16_t local_port, remote_port;
                            if (sessions[i].addr_family == AF_INET) { // Block start for AF_INET
                                local_port = ((const struct sockaddr_in *)&sessions[i].local_addr)->sin_port;
                                remote_port = ((const struct sockaddr_in *)&sessions[i].remote_addr)->sin_port;
                            } else { // AF_INET6
                                const struct sockaddr_in6 *temp_local_in6_ptr = (const struct sockaddr_in6 *)&sessions[i].local_addr;
                                const struct sockaddr_in6 *temp_remote_in6_ptr = (const struct sockaddr_in6 *)&sessions[i].remote_addr;
                                local_port = temp_local_in6_ptr->sin6_port;
                                remote_port = temp_remote_in6_ptr->sin6_port;
                            } // Block end for port extraction
                            log_to_file("שגיאה: חיבור נכשל לסשן %s:%d -> %s:%d: %s (דומיין: %s). סוגר סשן.", 
                                        addr_to_string(&sessions[i].local_addr, local_ip_str, sizeof(local_ip_str)), ntohs(local_port),
                                        addr_to_string(&sessions[i].remote_addr, remote_ip_str, sizeof(remote_ip_str)), ntohs(remote_port),
                                        strerror(error),
                                        sessions[i].hostname[0] != '\0' ? sessions[i].hostname : "לא ידוע");
                            close(sessions[i].remote_socket_fd);
                            sessions[i].remote_socket_fd = -1; 
                            sessions[i].state = TCP_STATE_CLOSED;
                        }
                    } else if (errno != EINPROGRESS && errno != EAGAIN && errno != EWOULDBLOCK) { // שגיאת getsockopt אמיתית
                        char local_ip_str[INET6_ADDRSTRLEN], remote_ip_str[INET6_ADDRSTRLEN];
                        uint16_t local_port, remote_port;
                        if (sessions[i].addr_family == AF_INET) { // Block start for AF_INET
                            local_port = ((const struct sockaddr_in *)&sessions[i].local_addr)->sin_port;
                            remote_port = ((const struct sockaddr_in *)&sessions[i].remote_addr)->sin_port;
                        } else { // AF_INET6
                            const struct sockaddr_in6 *temp_local_in6_ptr = (const struct sockaddr_in6 *)&sessions[i].local_addr;
                            const struct sockaddr_in6 *temp_remote_in6_ptr = (const struct sockaddr_in6 *)&sessions[i].remote_addr;
                            local_port = temp_local_in6_ptr->sin6_port;
                            remote_port = temp_remote_in6_ptr->sin6_port;
                        } // Block end for port extraction
                        log_to_file("שגיאת Getsockopt בבדיקת סטטוס חיבור: %s (דומיין: %s). סוגר סשן. %s:%d -> %s:%d", strerror(errno),
                                    sessions[i].hostname[0] != '\0' ? sessions[i].hostname : "לא ידוע",
                                    addr_to_string(&sessions[i].local_addr, local_ip_str, sizeof(local_ip_str)), ntohs(local_port),
                                    addr_to_string(&sessions[i].remote_addr, remote_ip_str, sizeof(remote_ip_str)), ntohs(remote_port));
                        close(sessions[i].remote_socket_fd);
                        sessions[i].remote_socket_fd = -1; 
                        sessions[i].state = TCP_STATE_CLOSED;
                    }
                } // SYN_SENT check end
                
                // הוסף סוקט ל-poll רק אם הוא מחובר או ממתין לחיבור
                fds[num_fds].fd = sessions[i].remote_socket_fd;
                fds[num_fds].events = POLLIN; // תמיד נרצה לקבל נתונים
                if (sessions[i].state == TCP_STATE_SYN_SENT) { // Block start for SYN_SENT event
                     fds[num_fds].events |= POLLOUT; // גם לחיבורים ב-SYN_SENT כדי לדעת מתי הסוקט מוכן לכתיבה (חיבור הושלם)
                } // Block end for SYN_SENT event
                num_fds++;
            } // Check if session is active end
        } // Sessions loop end

        int ret = poll(fds, num_fds, 100); // 100ms timeout
        if (ret < 0) { // Poll error start
            if (errno == EINTR) {
                continue; 
            }
            log_to_file("שגיאת Poll: %s", strerror(errno));
            break;
        } // Poll error end
        if (ret == 0) { // Poll timeout start
            cleanup_sessions();
            continue;
        } // Poll timeout end
        */
       
        pcapdroid_t pd = {
            .sdk_ver = sdk,
            .env = env,
            .capture_service = vpn,
            .cb = {
                    .get_libprog_path = getLibprogPath,
                    .load_blacklists_info = loadBlacklistsInfo,
                    .send_stats_dump = sendStatsDump,
                    .send_connections_dump = sendConnectionsDump,
                    .send_pcap_dump = sendPcapDump,
                    .stop_pcap_dump = stopPcapDump,
                    .notify_service_status = notifyServiceStatus,
                    .notify_blacklists_loaded = notifyBlacklistsLoaded,
                    .dump_payload_chunk = dumpPayloadChunk,
                    .clear_payload_chunks = clearPayloadChunks,
            },
            .mitm_addon_uid = getIntPref(env, vpn, "getMitmAddonUid"),
            .vpn_capture = (bool) getIntPref(env, vpn, "isVpnCapture"),
           // .pcap_file_capture = (bool) getIntPref(env, vpn, "isPcapFileCapture"),
            .payload_mode = (payload_mode_t) getIntPref(env, vpn, "getPayloadMode"),
          //  .pcap_dump = {
                //    .enabled = (bool) getIntPref(env, vpn, "pcapDumpEnabled"),
             //       .dump_extensions = (bool)getIntPref(env, vpn, "dumpExtensionsEnabled"),
                //    .pcapng_format = (bool)getIntPref(env, vpn, "isPcapngEnabled"),
                //    .snaplen = getIntPref(env, vpn, "getSnaplen"),
              //      .max_pkts_per_flow = getIntPref(env, vpn, "getMaxPktsPerFlow"),
          //          .max_dump_size = getIntPref(env, vpn, "getMaxDumpSize"),
          //  },
            .socks5 = {
                    .enabled = (bool) getIntPref(env, vpn, "getSocks5Enabled"),
                    .proxy_ip = getIPPref(env, vpn, "getSocks5ProxyAddress", &pd.socks5.proxy_ipver),
                    .proxy_port = htons(getIntPref(env, vpn, "getSocks5ProxyPort")),
            },
            .malware_detection = {
                    .enabled = (bool) getIntPref(env, vpn, "malwareDetectionEnabled"),
                     // .enabled = false,
            },
            .firewall = {
                    .enabled = (bool) getIntPref(env, vpn, "firewallEnabled"),
            },
            .tls_decryption = {
                    .enabled = (bool) getIntPref(env, vpn, "isTlsDecryptionEnabled"),
            }
    };

    if(pd.socks5.enabled)
        getSocks5ProxyAuth(&pd);

    if(pd.vpn_capture)
        pd.vpn.tunfd = tunfd;

    getStringPref(&pd, "getWorkingDir", pd.cachedir, sizeof(pd.cachedir));
    strcat(pd.cachedir, "/");
    pd.cachedir_len = strlen(pd.cachedir);

    getStringPref(&pd, "getPersistentDir", pd.filesdir, sizeof(pd.filesdir));
    strcat(pd.filesdir, "/");
    pd.filesdir_len = strlen(pd.filesdir);

    global_pd = &pd;
    jni_thread = pthread_self();
    logcallback = log_callback;
    signal(SIGPIPE, SIG_IGN);

    // Run the capture
    pd_run(&pd);

    global_pd = NULL;
    logcallback = NULL;

#if 0
    // free JNI local objects to ease references leak detection
    for(int i=0; i<sizeof(cls)/sizeof(jclass); i++) {
        jclass cur = ((jclass*)&cls)[i];
        (*env)->DeleteLocalRef(env, cur);
    }
    for(int i=0; i<sizeof(enums)/sizeof(jobject); i++) {
        jobject cur = ((jobject*)&enums)[i];
        (*env)->DeleteLocalRef(env, cur);
    }

    // at this point the local reference table should only contain 2 entries (VMDebug + Thread)
    jniDumpReferences(env);
#endif

#ifdef PCAPDROID_TRACK_ALLOCS
    log_to_file(get_allocs_summary());
#endif
/*
        zdtun_t *zdt;
    char buffer[VPN_BUFFER_SIZE];
    u_int64_t next_purge_ms;
    
    

    int flags = fcntl(pd.vpn.tunfd, F_GETFL, 0);
    if (flags < 0 || fcntl(pd.vpn.tunfd, F_SETFL, flags & ~O_NONBLOCK) < 0) {
         log_to_file("fcntl ~O_NONBLOCK error [%d]: %s", errno,
                    strerror(errno));
       // return (-1);
       return;
    }

#if ANDROID
    pd.vpn.resolver = init_uid_resolver(pd.sdk_ver, pd.env, pd.capture_service);
    //pd.vpn.known_dns_servers = blacklist_init();
    pd.vpn.block_quic_mode = getIntPref(pd.env, pd.capture_service, "getBlockQuickMode");

    pd.vpn.ipv4.enabled = (bool) getIntPref(pd.env, pd.capture_service, "getIPv4Enabled");
    pd.vpn.ipv4.dns_server = getIPv4Pref(pd.env, pd.capture_service, "getDnsServer");
    pd.vpn.ipv4.internal_dns = getIPv4Pref(pd.env, pd.capture_service, "getVpnDns");

    pd.vpn.ipv6.enabled = (bool) getIntPref(pd.env, pd.capture_service, "getIPv6Enabled");
    pd.vpn.ipv6.dns_server = getIPv6Pref(pd.env, pd.capture_service, "getIpv6DnsServer");
#endif
        zdtun_callbacks_t callbacks = {
        .send_client = remote2vpn,
        .account_packet = update_conn_status,
        .on_socket_open = protectSocketCallback,
        .on_connection_open = handle_new_connection,
        .on_connection_close = connection_closed,
    };
    pcapdroid_t *pda=&pd;
    load_dns_servers(pda);
          
zdt = zdtun_init(&callbacks, pda);
    if(zdt == NULL) {
        log_to_file("zdtun_init failed");
        //return(-2);
        return;
    }*/
    /*
        // עיבוד אירועי התקן TUN
        if (fds[0].revents & POLLIN) { // TUN event start
            ssize_t len = read(tun_fd, tun_buf, sizeof(tun_buf));
            if (len > 0) { // Read successful
                zdtun_pkt_t pkt;
                zdtun_parse_pkt(zdt, tun_buf, len, &pkt);
                handle_ip_packet(tun_buf, len);
            } else if (len < 0 && errno != EAGAIN && errno != EWOULDBLOCK) { // Read error
                log_to_file("שגיאה בקריאה מ-TUN: %s", strerror(errno));
                //running = 0; 
            }
        } // TUN event end

        // עיבוד אירועי סוקט אינטרנט
        for (int i = 1; i < num_fds; i++) { // Internet socket loop start
            tcp_session_t *current_session = NULL;
            for (int s_idx = 0; s_idx < MAX_SESSIONS; s_idx++) { // Find session for FD
                if (sessions[s_idx].remote_socket_fd == fds[i].fd) {
                    current_session = &sessions[s_idx];
                    break;
                }
            } // End of find session for FD

            if (!current_session) { // No session found
                log_to_file("אזהרה: FD %d שנסרק אינו משויך לאף סשן. סוגר.", fds[i].fd);
                close(fds[i].fd);
                continue;
            }

            // אם זהו סוקט TCP במצב SYN_SENT, ו-POLLOUT מצביע על חיבור שהושלם
            if ((current_session->state == TCP_STATE_SYN_SENT) && (fds[i].revents & POLLOUT)) { // POLLOUT for SYN_SENT start
                 int error = 0;
                 socklen_t len = sizeof(error);
                 if (getsockopt(current_session->remote_socket_fd, SOL_SOCKET, SO_ERROR, &error, &len) == 0 && error == 0) { // Getsockopt successful and no error
                     char local_ip_str[INET6_ADDRSTRLEN], remote_ip_str[INET6_ADDRSTRLEN];
                     uint16_t local_port, remote_port;
                     if (current_session->addr_family == AF_INET) { // Block start for AF_INET
                         local_port = ((const struct sockaddr_in *)&current_session->local_addr)->sin_port;
                         remote_port = ((const struct sockaddr_in *)&current_session->remote_addr)->sin_port;
                     } else { // AF_INET6
                         const struct sockaddr_in6 *temp_local_in6_ptr = (const struct sockaddr_in6 *)&current_session->local_addr;
                         const struct sockaddr_in6 *temp_remote_in6_ptr = (const struct sockaddr_in6 *)&current_session->remote_addr;
                         local_port = temp_local_in6_ptr->sin6_port;
                         remote_port = temp_remote_in6_ptr->sin6_port;
                     } // Block end for port extraction
                     log_to_file("TCP: חיבור מוצלח לשרת %s:%d (FD: %d) עבור סשן %s:%d -> %s:%d (דומיין: %s, AF: %d) (מ-POLLOUT).",
                                 addr_to_string(&current_session->remote_addr, remote_ip_str, sizeof(remote_ip_str)), ntohs(remote_port),
                                 current_session->remote_socket_fd,
                                 addr_to_string(&current_session->local_addr, local_ip_str, sizeof(local_ip_str)), ntohs(local_port),
                                 addr_to_string(&current_session->remote_addr, remote_ip_str, sizeof(remote_ip_str)), ntohs(remote_port),
                                 current_session->hostname[0] != '\0' ? current_session->hostname : "לא ידוע",
                                 current_session->addr_family);
                     current_session->state = TCP_STATE_SYN_RECEIVED;
                     
                     // שליחת SYN-ACK בחזרה ללקוח TUN
                     struct tcp_header synack_tcph;
                     memset(&synack_tcph, 0, sizeof(synack_tcph));
                     synack_tcph.th_sport = remote_port; 
                     synack_tcph.th_dport = local_port; 
                     synack_tcph.th_seq = htonl(current_session->server_seq_num); 
                     synack_tcph.th_ack = htonl(current_session->client_seq_num); 
                     synack_tcph.th_flags = TH_SYN | TH_ACK; 
                     synack_tcph.th_win = current_session->window_size; 

                     uint8_t tcp_options[40]; 
                     int options_len = 0;
                     uint16_t mss_to_announce = VPN_RECOMMENDED_MSS; 
                     options_len += create_mss_option(tcp_options + options_len, mss_to_announce);
                     synack_tcph.th_off = (sizeof(struct tcp_header) + options_len + 3) / 4; 
                     synack_tcph.th_off = (synack_tcph.th_off < 5) ? 5 : synack_tcph.th_off; 

                     synack_tcph.th_sum = 0; 
                     if (current_session->addr_family == AF_INET) { // Block start for AF_INET
                         synack_tcph.th_sum = transport_checksum_ipv4(
                             ((const struct sockaddr_in *)&current_session->remote_addr)->sin_addr.s_addr,
                             ((const struct sockaddr_in *)&current_session->local_addr)->sin_addr.s_addr,
                             IPPROTO_TCP, &synack_tcph, sizeof(struct tcp_header) + options_len, NULL, 0);
                     } else { // AF_INET6
                          struct in6_addr temp_remote_in6_addr_copy = ((const struct sockaddr_in6 *)&current_session->remote_addr)->sin6_addr;
                          struct in6_addr temp_local_in6_addr_copy = ((const struct sockaddr_in6 *)&current_session->local_addr)->sin6_addr;
                          synack_tcph.th_sum = transport_checksum_ipv6(
                             &temp_remote_in6_addr_copy, 
                             &temp_local_in6_addr_copy, 
                             IPPROTO_TCP, sizeof(struct tcp_header) + options_len,
                             &synack_tcph, sizeof(struct tcp_header) + options_len, NULL, 0);
                     } // Block end for address family
                     send_packet_to_tun(&current_session->remote_addr, &current_session->local_addr, IPPROTO_TCP,
                                        &synack_tcph, sizeof(struct tcp_header) + options_len, NULL, 0, ++current_session->ip_id_counter);
                 } else { // חיבור נכשל (POLLOUT עם שגיאה)
                     char local_ip_str[INET6_ADDRSTRLEN], remote_ip_str[INET6_ADDRSTRLEN];
                     uint16_t local_port, remote_port;
                     if (current_session->addr_family == AF_INET) { // Block start for AF_INET
                         local_port = ((const struct sockaddr_in *)&current_session->local_addr)->sin_port;
                         remote_port = ((const struct sockaddr_in *)&current_session->remote_addr)->sin_port;
                     } else { // AF_INET6
                         const struct sockaddr_in6 *temp_local_in6_ptr = (const struct sockaddr_in6 *)&current_session->local_addr;
                         const struct sockaddr_in6 *temp_remote_in6_ptr = (const struct sockaddr_in6 *)&current_session->remote_addr;
                         local_port = temp_local_in6_ptr->sin6_port;
                         remote_port = temp_remote_in6_ptr->sin6_port;
                     } // Block end for port extraction
                     log_to_file("שגיאה: חיבור נכשל לסשן %s:%d -> %s:%d (מ-POLLOUT): %s (דומיין: %s). סוגר סשן.", 
                                 addr_to_string(&current_session->local_addr, local_ip_str, sizeof(local_ip_str)), ntohs(local_port),
                                 addr_to_string(&current_session->remote_addr, remote_ip_str, sizeof(remote_ip_str)), ntohs(remote_port),
                                 strerror(error),
                                 current_session->hostname[0] != '\0' ? current_session->hostname : "לא ידוע");
                     close(current_session->remote_socket_fd);
                     current_session->remote_socket_fd = -1; 
                     current_session->state = TCP_STATE_CLOSED;
                 }
            } // POLLOUT for SYN_SENT end


            if (fds[i].revents & POLLIN) { // POLLIN event start
                ssize_t recv_len = recv(current_session->remote_socket_fd, internet_buf, sizeof(internet_buf), 0);
                if (recv_len > 0) { // Data received
                    char local_ip_str[INET6_ADDRSTRLEN], remote_ip_str[INET6_ADDRSTRLEN];
                    uint16_t local_port, remote_port;
                    if (current_session->addr_family == AF_INET) { // Block start for AF_INET
                        local_port = ((const struct sockaddr_in *)&current_session->local_addr)->sin_port;
                        remote_port = ((const struct sockaddr_in *)&current_session->remote_addr)->sin_port;
                    } else { // AF_INET6
                        const struct sockaddr_in6 *temp_local_in6_ptr = (const struct sockaddr_in6 *)&current_session->local_addr;
                        const struct sockaddr_in6 *temp_remote_in6_ptr = (const struct sockaddr_in6 *)&current_session->remote_addr;
                        local_port = temp_local_in6_ptr->sin6_port;
                        remote_port = temp_remote_in6_ptr->sin6_port;
                    } // Block end for port extraction
                    log_to_file("TCP: התקבלו %zd בתים משרת האינטרנט עבור סשן %s:%d -> %s:%d (דומיין: %s)",
                                recv_len, addr_to_string(&current_session->local_addr, local_ip_str, sizeof(local_ip_str)),
                                ntohs(local_port),
                                addr_to_string(&current_session->remote_addr, remote_ip_str, sizeof(remote_ip_str)),
                                ntohs(remote_port),
                                current_session->hostname[0] != '\0' ? current_session->hostname : "לא ידוע");

                    struct tcp_header reply_tcph;
                    memset(&reply_tcph, 0, sizeof(reply_tcph));
                    reply_tcph.th_sport = remote_port; 
                    reply_tcph.th_dport = local_port; 
                    
                    // סביר להניח שזה ה-ACK/PSH הראשון מהשרת. מספר הרצף ההתחלתי שנקבע עבור השרת + אורך הנתונים
                    reply_tcph.th_seq = htonl(current_session->server_seq_num);
                    reply_tcph.th_ack = htonl(current_session->client_seq_num); // ה-ACK האחרון שקיבלנו מהלקוח
                    
                    reply_tcph.th_off = 5; 
                    reply_tcph.th_flags = TH_ACK | TH_PUSH; 
                    reply_tcph.th_win = current_session->window_size; 
                    reply_tcph.th_sum = 0;

                    if (current_session->addr_family == AF_INET) { // Block start for AF_INET
                        reply_tcph.th_sum = transport_checksum_ipv4(
                            ((const struct sockaddr_in *)&current_session->remote_addr)->sin_addr.s_addr,
                            ((const struct sockaddr_in *)&current_session->local_addr)->sin_addr.s_addr,
                            IPPROTO_TCP, &reply_tcph, sizeof(reply_tcph),
                            (uint8_t*)internet_buf, recv_len);
                    } else { // AF_INET6
                        struct in6_addr temp_remote_in6_addr_copy = ((const struct sockaddr_in6 *)&current_session->remote_addr)->sin6_addr;
                        struct in6_addr temp_local_in6_addr_copy = ((const struct sockaddr_in6 *)&current_session->local_addr)->sin6_addr;
                        reply_tcph.th_sum = transport_checksum_ipv6(
                            &temp_remote_in6_addr_copy, 
                            &temp_local_in6_addr_copy, 
                            IPPROTO_TCP, sizeof(reply_tcph) + recv_len, &reply_tcph, sizeof(reply_tcph),
                            (uint8_t*)internet_buf, recv_len);
                    } // Block end for address family

                    send_packet_to_tun(&current_session->remote_addr, &current_session->local_addr, IPPROTO_TCP,
                                       &reply_tcph, sizeof(reply_tcph),
                                       (uint8_t*)internet_buf, recv_len,
                                       ++current_session->ip_id_counter);

                    current_session->server_seq_num += recv_len; // עדכון מספר הרצף של השרת
                    current_session->last_active = time(NULL);

                } else if (recv_len == 0) { // FIN from remote server
                    char local_ip_str[INET6_ADDRSTRLEN], remote_ip_str[INET6_ADDRSTRLEN];
                    uint16_t local_port, remote_port;
                    if (current_session->addr_family == AF_INET) { // Block start for AF_INET
                        local_port = ((const struct sockaddr_in *)&current_session->local_addr)->sin_port;
                        remote_port = ((const struct sockaddr_in *)&current_session->remote_addr)->sin_port;
                    } else { // AF_INET6
                        const struct sockaddr_in6 *temp_local_in6_ptr = (const struct sockaddr_in6 *)&current_session->local_addr;
                        const struct sockaddr_in6 *temp_remote_in6_ptr = (const struct sockaddr_in6 *)&current_session->remote_addr;
                        local_port = temp_local_in6_ptr->sin6_port;
                        remote_port = temp_remote_in6_ptr->sin6_port;
                    } // Block end for port extraction
                    log_to_file("TCP: שרת האינטרנט סגר חיבור עבור סשן %s:%d -> %s:%d (דומיין: %s).",
                                addr_to_string(&current_session->local_addr, local_ip_str, sizeof(local_ip_str)),
                                ntohs(local_port),
                                addr_to_string(&current_session->remote_addr, remote_ip_str, sizeof(remote_ip_str)),
                                ntohs(remote_port),
                                current_session->hostname[0] != '\0' ? current_session->hostname : "לא ידוע");

                    struct tcp_header fin_tcph;
                    memset(&fin_tcph, 0, sizeof(fin_tcph));
                    fin_tcph.th_sport = remote_port;
                    fin_tcph.th_dport = local_port;
                    
                    fin_tcph.th_seq = htonl(current_session->server_seq_num); 
                    fin_tcph.th_ack = htonl(current_session->client_seq_num); 
                    
                    fin_tcph.th_off = 5;
                    fin_tcph.th_flags = TH_FIN | TH_ACK; 
                    fin_tcph.th_win = current_session->window_size;
                    fin_tcph.th_sum = 0;
                    if (current_session->addr_family == AF_INET) { // Block start for AF_INET
                        fin_tcph.th_sum = transport_checksum_ipv4(
                            ((const struct sockaddr_in *)&current_session->remote_addr)->sin_addr.s_addr,
                            ((const struct sockaddr_in *)&current_session->local_addr)->sin_addr.s_addr,
                            IPPROTO_TCP, &fin_tcph, sizeof(fin_tcph), NULL, 0);
                    } else { // AF_INET6
                        struct in6_addr temp_remote_in6_addr_copy = ((const struct sockaddr_in6 *)&current_session->remote_addr)->sin6_addr;
                        struct in6_addr temp_local_in6_addr_copy = ((const struct sockaddr_in6 *)&current_session->local_addr)->sin6_addr;
                        fin_tcph.th_sum = transport_checksum_ipv6(
                            &temp_remote_in6_addr_copy, 
                            &temp_local_in6_addr_copy, 
                            IPPROTO_TCP, sizeof(fin_tcph), &fin_tcph, sizeof(fin_tcph), NULL, 0);
                    } // Block end for address family
                    send_packet_to_tun(&current_session->remote_addr, &current_session->local_addr, IPPROTO_TCP,
                                       &fin_tcph, sizeof(fin_tcph), NULL, 0, ++current_session->ip_id_counter);
                    
                    current_session->server_seq_num++; // FIN צורך 1 בייט
                    current_session->state = TCP_STATE_CLOSE_WAIT; 
                    // לא סוגרים את הסוקט המרוחק עד שהלקוח שולח ACK על ה-FIN שלנו (LAST_ACK) או FIN משלו (CLOSING)
                    // close(current_session->remote_socket_fd); 
                    // current_session->remote_socket_fd = -1; 
                    current_session->last_active = time(NULL);

                } else if (recv_len < 0 && errno != EAGAIN && errno != EWOULDBLOCK) { // Recv error
                    char local_ip_str[INET6_ADDRSTRLEN], remote_ip_str[INET6_ADDRSTRLEN];
                    uint16_t local_port, remote_port;
                    if (current_session->addr_family == AF_INET) { // Block start for AF_INET
                        local_port = ((const struct sockaddr_in *)&current_session->local_addr)->sin_port;
                        remote_port = ((const struct sockaddr_in *)&current_session->remote_addr)->sin_port;
                    } else { // AF_INET6
                        const struct sockaddr_in6 *temp_local_in6_ptr = (const struct sockaddr_in6 *)&current_session->local_addr;
                        const struct sockaddr_in6 *temp_remote_in6_ptr = (const struct sockaddr_in6 *)&current_session->remote_addr;
                        local_port = temp_local_in6_ptr->sin6_port;
                        remote_port = temp_remote_in6_ptr->sin6_port;
                    } // Block end for port extraction
                    log_to_file("TCP: שגיאה בקבלת נתונים מהאינטרנט: %s (דומיין: %s). סוגר סשן. %s:%d -> %s:%d", strerror(errno),
                                current_session->hostname[0] != '\0' ? current_session->hostname : "לא ידוע",
                                addr_to_string(&current_session->local_addr, local_ip_str, sizeof(local_ip_str)), ntohs(local_port),
                                addr_to_string(&current_session->remote_addr, remote_ip_str, sizeof(remote_ip_str)), ntohs(remote_port));

                    struct tcp_header rst_tcph;
                    memset(&rst_tcph, 0, sizeof(rst_tcph));
                    rst_tcph.th_sport = remote_port;
                    rst_tcph.th_dport = local_port;
                    rst_tcph.th_seq = htonl(current_session->server_seq_num);
                    rst_tcph.th_ack = htonl(current_session->client_seq_num);
                    rst_tcph.th_off = 5;
                    rst_tcph.th_flags = TH_RST | TH_ACK;
                    rst_tcph.th_win = htons(0);
                    rst_tcph.th_sum = 0;
                    if (current_session->addr_family == AF_INET) { // Block start for AF_INET
                        rst_tcph.th_sum = transport_checksum_ipv4(
                            ((const struct sockaddr_in *)&current_session->remote_addr)->sin_addr.s_addr,
                            ((const struct sockaddr_in *)&current_session->local_addr)->sin_addr.s_addr,
                            IPPROTO_TCP, &rst_tcph, sizeof(rst_tcph), NULL, 0);
                    } else { // AF_INET6
                        struct in6_addr temp_remote_in6_addr_copy = ((const struct sockaddr_in6 *)&current_session->remote_addr)->sin6_addr;
                        struct in6_addr temp_local_in6_addr_copy = ((const struct sockaddr_in6 *)&current_session->local_addr)->sin6_addr;
                        rst_tcph.th_sum = transport_checksum_ipv6(
                            &temp_remote_in6_addr_copy, 
                            &temp_local_in6_addr_copy, 
                            IPPROTO_TCP, sizeof(rst_tcph), &rst_tcph, sizeof(rst_tcph), NULL, 0);
                    } // Block end for address family
                    send_packet_to_tun(&current_session->remote_addr, &current_session->local_addr, IPPROTO_TCP, &rst_tcph, sizeof(rst_tcph), NULL, 0, ++current_session->ip_id_counter);
                } // End of recv error
            } else if (fds[i].revents & (POLLERR | POLLHUP | POLLNVAL)) { // Socket error/hangup
                char local_ip_str[INET6_ADDRSTRLEN], remote_ip_str[INET6_ADDRSTRLEN];
                uint16_t local_port, remote_port;
                if (current_session->addr_family == AF_INET) { // Block start for AF_INET
                    local_port = ((const struct sockaddr_in *)&current_session->local_addr)->sin_port;
                    remote_port = ((const struct sockaddr_in *)&current_session->remote_addr)->sin_port;
                } else { // AF_INET6
                    const struct sockaddr_in6 *temp_local_in6_ptr = (const struct sockaddr_in6 *)&current_session->local_addr;
                    const struct sockaddr_in6 *temp_remote_in6_ptr = (const struct sockaddr_in6 *)&current_session->remote_addr;
                    local_port = temp_local_in6_ptr->sin6_port;
                    remote_port = temp_remote_in6_ptr->sin6_port;
                } // Block end for port extraction
                log_to_file("TCP: שגיאת סוקט/ניתוק עבור FD %d. סוגר סשן. (דומיין: %s) %s:%d -> %s:%d", fds[i].fd,
                            current_session->hostname[0] != '\0' ? current_session->hostname : "לא ידוע",
                            addr_to_string(&current_session->local_addr, local_ip_str, sizeof(local_ip_str)), ntohs(local_port),
                            addr_to_string(&current_session->remote_addr, remote_ip_str, sizeof(remote_ip_str)), ntohs(remote_port));
                if (current_session->remote_socket_fd != -1) { // Close socket if open
                    close(current_session->remote_socket_fd);
                }
                current_session->remote_socket_fd = -1;
                current_session->state = TCP_STATE_CLOSED;
                
                struct tcp_header rst_tcph;
                memset(&rst_tcph, 0, sizeof(rst_tcph));
                rst_tcph.th_sport = remote_port;
                rst_tcph.th_dport = local_port;
                rst_tcph.th_seq = htonl(current_session->server_seq_num);
                rst_tcph.th_ack = htonl(current_session->client_seq_num);
                rst_tcph.th_off = 5;
                rst_tcph.th_flags = TH_RST | TH_ACK;
                rst_tcph.th_win = htons(0);
                rst_tcph.th_sum = 0;
                if (current_session->addr_family == AF_INET) { // Block start for AF_INET
                    rst_tcph.th_sum = transport_checksum_ipv4(
                        ((const struct sockaddr_in *)&current_session->remote_addr)->sin_addr.s_addr,
                        ((const struct sockaddr_in *)&current_session->local_addr)->sin_addr.s_addr,
                        IPPROTO_TCP, &rst_tcph, sizeof(rst_tcph), NULL, 0);
                } else { // AF_INET6
                    struct in6_addr temp_remote_in6_addr_copy = ((const struct sockaddr_in6 *)&current_session->remote_addr)->sin6_addr;
                    struct in6_addr temp_local_in6_addr_copy = ((const struct sockaddr_in6 *)&current_session->local_addr)->sin6_addr;
                    rst_tcph.th_sum = transport_checksum_ipv6(
                        &temp_remote_in6_addr_copy, 
                        &temp_local_in6_addr_copy, 
                        IPPROTO_TCP, sizeof(rst_tcph), &rst_tcph, sizeof(rst_tcph), NULL, 0);
                } // Block end for address family
                send_packet_to_tun(&current_session->remote_addr, &current_session->local_addr, IPPROTO_TCP, &rst_tcph, sizeof(rst_tcph), NULL, 0, ++current_session->ip_id_counter);
            } // End of socket error/hangup
        } // End of POLLIN event
    } // Internet socket loop end
    
    // Cleanup resources outside the main loop only after `running` becomes 0
    // and the loop has naturally exited.
    // However, the current code has `cleanup_sessions()` inside the loop which is fine for timeout handling.
    
    free(fds); // Free pollfd array

    // סגירת TUN FD וכל הסוקטים הפתוחים
    // Ensure `tun_fd_global` is closed ONLY once.
    if (tun_fd_global >= 0) { // לוודא ש-FD עדיין תקין לפני סגירה
        close(tun_fd_global);
        tun_fd_global = -1; // איפוס
    }
    for (int i = 0; i < MAX_SESSIONS; i++) { // Loop to close remaining sockets
        if (sessions[i].remote_socket_fd != -1) {
            close(sessions[i].remote_socket_fd);
            sessions[i].remote_socket_fd = -1;
        }
    } // End of loop to close remaining sockets
    */
    log_to_file("ה-thread המקורי יצא בצורה חלקה.");
} // End of nativeStart function

JNIEXPORT void JNICALL Java_com_domain_filter_MyVpnService_nativeStop(JNIEnv *env, jobject thiz) { // Function start
    //running = 0;
    log_to_file("nativeStop נקרא.");
    // No need to close tun_fd_global or sessions here explicitly, as nativeStart's cleanup will handle it
    // once the `running` flag causes the loop to exit.
} // End of nativeStop function

