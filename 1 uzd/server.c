#ifdef _WIN32
#include <winsock2.h>
#define socklen_t int
#else
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

//Constants
#define WORD_LENGTH 5
#define GUESS_COUNT 6
#define BUFFER_LENGTH WORD_LENGTH + 1

#define END_SYMBOL '#'
#define CORRECT_SYMBOL_AND_POSITION '+'
#define CORRECT_SYMBOL_NOT_POSITION '*'
#define INCORRECT_SYMBOL '-'

#define MSG_USAGE "USAGE: %s <port>\n"
#define MSG_CONNECTED "Connected:  %s\n"
#define MSG_DISCONNECTED "IP: %s disconnected"
#define MSG_SERVER_SENT "Server sent: %s\n"
#define MSG_GAME_OVER "Game over! IP: %s disconnected\n"
#define MSG_SREVER_STARTED "Server started!\n"
#define MSG_WSA_FAILED "WSAStartup failed!\n"

#define ERR_INVALID_PORT "ERROR #1: invalid port specified.\n"
#define ERR_CANNOT_CREATE "ERROR #2: cannot create listening socket.\n"
#define ERR_CANNOT_BIND "ERROR #3: cannot bind listening socket.\n"
#define ERR_CANNOT_LISTEN "ERROR #4: error while listening.\n"
#define ERR_CANNOT_SELECT "ERROR #5: cannot select socket.\n"
#define ERR_CANNOT_ACCEPT "ERROR #6: cannot accept the connection.\n"
#define ERR_INCORRECT_DATA_SENT "ERROR #7: the sent data is incorrect.\n"
#define ERR_INCORRECT_DATA_RECEIVED "ERROR #8: the received data is incorrect.\n"

int main(int argc, char *argv []){
#ifdef _WIN32
    WSADATA data;
#endif
    unsigned int port;

    // Socket that is waiting for connection
    int l_socket; 

    // Socket for the connected client
    int c_socket;

    // Struct for server address
    struct sockaddr_in servaddr; 

    // Struct for connected client's address
    struct sockaddr_in clientaddr; 

    socklen_t clientaddrlen;

    int s_len;
    int r_len;

    char buffer[BUFFER_LENGTH];

    // Today's word:
    char word_to_guess[BUFFER_LENGTH] = "CISCO";   

    if (argc != 2){
        fprintf(stderr, MSG_USAGE, argv[0]);
        exit(1);
    }

    // Converting the second argument from ascii symbols to integer (port)
    port = atoi(argv[1]);
    if ((port < 1) || (port > 65535)){
        fprintf(stderr, ERR_INVALID_PORT);
        exit(1);
    }

#ifdef _WIN32
    int wsa_result = WSAStartup(MAKEWORD(2, 2), &data);
    if (wsa_result != 0){
        printf(MSG_WSA_FAILED);
        exit(1);
    }
#endif

    // Creating the socket
    if ((l_socket = socket(AF_INET, SOCK_STREAM, 0)) < 0){
        fprintf(stderr, ERR_CANNOT_CREATE);
        exit(1);
    }
  
    // Clearing the server address struct
    memset(&servaddr, 0, sizeof(servaddr));

    // IPv4 Protocol
    servaddr.sin_family = AF_INET;

    // Specifying the expected ip addresses (in this case - any address is acceptable)
    servaddr.sin_addr.s_addr = htonl(INADDR_ANY);

    // Specifying the port
    servaddr.sin_port = htons(port);

    // Binding the server's address with the socket
    if (bind(l_socket, (struct sockaddr*)&servaddr, sizeof(servaddr)) < 0){
        fprintf(stderr, ERR_CANNOT_BIND);
        exit(1);
    }

    // Specifying that l_socket will wait for connections (max 5 clients can wait in line)
    if (listen(l_socket, 5) < 0){
        fprintf(stderr, ERR_CANNOT_LISTEN);
        exit(1);
    }

    printf(MSG_SREVER_STARTED);

    for(;;){
        // Clearing the buffer and clinet address
        memset(&clientaddr, 0, sizeof(clientaddr));
        memset(&buffer, 0, sizeof(buffer));

        // Waiting for client connections
        clientaddrlen = sizeof(struct sockaddr);
        if ((c_socket = accept(l_socket, (struct sockaddr*)&clientaddr, &clientaddrlen)) < 0){
            fprintf(stderr, ERR_CANNOT_ACCEPT);
            exit(1);
        }

        // Sending the word length and the guess count to the clinet
        buffer[0] = GUESS_COUNT + '0';
        buffer[1] = WORD_LENGTH + '0';
        buffer[2] = CORRECT_SYMBOL_AND_POSITION;
        buffer[3] = CORRECT_SYMBOL_NOT_POSITION;
        buffer[4] = INCORRECT_SYMBOL;
        buffer[5] = END_SYMBOL;

        if((s_len = send(c_socket, buffer, 6, 0)) != 6){
            fprintf(stderr, ERR_INCORRECT_DATA_SENT);
            printf(MSG_DISCONNECTED, inet_ntoa(clientaddr.sin_addr));          
            close(c_socket);
            break;
        }

        for(;;){
            memset(&buffer, 0, sizeof(buffer));

            // Receiving the client's guess
            if((r_len = recv(c_socket, buffer, sizeof(buffer), 0)) < 0){
                fprintf(stderr, ERR_INCORRECT_DATA_RECEIVED);
                printf(MSG_DISCONNECTED, inet_ntoa(clientaddr.sin_addr));          
                close(c_socket);
                break;
            }

            // If the game ended in a win or in a loss
            if(r_len == 1 && buffer[0] == END_SYMBOL){
                // Sending the word of the day for the client
                if((s_len = send(c_socket, word_to_guess, WORD_LENGTH, 0)) != WORD_LENGTH){
                    fprintf(stderr, ERR_INCORRECT_DATA_SENT);
                    printf(MSG_DISCONNECTED, inet_ntoa(clientaddr.sin_addr));          
                    close(c_socket);
                    break;
                }
                
                printf(MSG_GAME_OVER, inet_ntoa(clientaddr.sin_addr));
                close(c_socket);
                break;
            }

            // Logic for checking the guess:
            int i;
            for(i = 0; i < WORD_LENGTH; ++i){
                // Case 1: the letter is in the correct position
                if(toupper(word_to_guess[i]) == toupper(buffer[i]))
                    buffer[i] = CORRECT_SYMBOL_AND_POSITION;
                else{
                    int j;
                    int is_partly_correct = 0;
                    for(j = 0; j < WORD_LENGTH; ++j){
                        // Case 2: the letter is in the word, but not in the correct position
                        if(toupper(word_to_guess[j]) == toupper(buffer[i])){
                            buffer[i] = CORRECT_SYMBOL_NOT_POSITION;
                            is_partly_correct = 1;
                        }
                    }

                    // Case 2: the letter is not in the word
                    if(is_partly_correct == 0)
                        buffer[i] = INCORRECT_SYMBOL;
                }
            }

            // Sending the clues back to the client
            if((s_len = send(c_socket, buffer, WORD_LENGTH, 0)) != WORD_LENGTH){
                fprintf(stderr, ERR_INCORRECT_DATA_SENT);
                printf(MSG_DISCONNECTED, inet_ntoa(clientaddr.sin_addr));          
                close(c_socket);
                break;
            }

            // Data sent to the user
            buffer[WORD_LENGTH] = '\0';
            printf(MSG_SERVER_SENT, buffer);
        }
    }

    return 0;
}
