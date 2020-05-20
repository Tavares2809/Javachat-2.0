/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sendfile.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.StringTokenizer;

/**
 *
 * @author juaninha
 */
public class SocketThread implements Runnable {

    Socket socket;
    MainForm main;
    DataInputStream dis;
    StringTokenizer st;
    String client, filesharing_username;

    private final int BUFFER_SIZE = 100;

    public SocketThread(Socket socket, MainForm main) {
        this.main = main;
        this.socket = socket;

        try {
            dis = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            main.appendMessage("[SocketThreadIOException]: " + e.getMessage());
        }
    }

    /*  Esta função pega o soquete do cliente na lista de soquetes do cliente e estabelece uma conexão  */
    private void createConnection(String receiver, String sender, String filename) {
        try {
            main.appendMessage("[createConnection]: criando conexão de compartilhamento de arquivos ");
            Socket s = main.getClientList(receiver);
            if (s != null) { // Client đã tồn tại
                main.appendMessage("[createConnection]: Socket OK");
                DataOutputStream dosS = new DataOutputStream(s.getOutputStream());
                main.appendMessage("[createConnection]: DataOutputStream OK");
                // Format:  CMD_FILE_XD [sender] [receiver] [filename]
                String format = "CMD_FILE_XD " + sender + " " + receiver + " " + filename;
                dosS.writeUTF(format);
                main.appendMessage("[createConnection]: " + format);
            } else {//O cliente não existe, envie de volta ao remetente que o destinatário não encontrou.
                main.appendMessage("[createConnection]: Cliente não encontrado '" + receiver + "'");
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF("CMD_SENDFILEERROR " + "Cliente '" + receiver +"'não foi encontrado na lista, garantindo que o usuário esteja online.!");
            }
        } catch (IOException e) {
            main.appendMessage("[createConnection]: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                /**
                 * Método de recebimento de dados do cliente *
                 */
                String data = dis.readUTF();
                st = new StringTokenizer(data);
                String CMD = st.nextToken();
                /**
                 * Verifique o CMD *
                 */
                switch (CMD) {
                    case "CMD_JOIN":
                        /**
                         * CMD_JOIN [NomeCliente] *
                         */
                        String clientUsername = st.nextToken();
                        client = clientUsername;
                        main.setClientList(clientUsername);
                        main.setSocketList(socket);
                        main.appendMessage("[Client]: " + clientUsername + " Entre na sala de bate-papo.");
                        break;

                    case "CMD_CHAT":
                        /**
                         * CMD_CHAT [de] [enviarPara] [mensagem] *
                         */
                        String from = st.nextToken();
                        String sendTo = st.nextToken();
                        String msg = "";
                        while (st.hasMoreTokens()) {
                            msg = msg + " " + st.nextToken();
                        }
                        Socket tsoc = main.getClientList(sendTo);
                        try {
                            DataOutputStream dos = new DataOutputStream(tsoc.getOutputStream());
                            /**
                             * CMD_MESSAGE *
                             */
                            String content = from + ": " + msg;
                            dos.writeUTF("CMD_MESSAGE " + content);
                            main.appendMessage("[Message]: De " + from + " Para "+ sendTo + " : " + msg);
                        } catch (IOException e) {
                            main.appendMessage("[IOException]: Não foi possível enviar a mensagem para " + sendTo);
                        }
                        break;

                    case "CMD_CHATALL":
                        /**
                         * CMD_CHATALL [from] [message] *
                         */
                        String chatall_from = st.nextToken();
                        String chatall_msg = "";
                        while (st.hasMoreTokens()) {
                            chatall_msg = chatall_msg + " " + st.nextToken();
                        }
                        String chatall_content = chatall_from + " " + chatall_msg;
                        for (int x = 0; x < main.clientList.size(); x++) {
                            if (!main.clientList.elementAt(x).equals(chatall_from)) {
                                try {
                                    Socket tsoc2 = (Socket) main.socketList.elementAt(x);
                                    DataOutputStream dos2 = new DataOutputStream(tsoc2.getOutputStream());
                                    dos2.writeUTF("CMD_MESSAGE " + chatall_content);
                                } catch (IOException e) {
                                    main.appendMessage("[CMD_CHATALL]: " + e.getMessage());
                                }
                            }
                        }
                        main.appendMessage("[CMD_CHATALL]: " + chatall_content);
                        break;

                    case "CMD_SHARINGSOCKET":
                        main.appendMessage("CMD_SHARINGSOCKET : O cliente estabelece um soquete para a conexão de compartilhamento de arquivos ... ");
                        String file_sharing_username = st.nextToken();
                        filesharing_username = file_sharing_username;
                        main.setClientFileSharingUsername(file_sharing_username);
                        main.setClientFileSharingSocket(socket);
                        main.appendMessage("CMD_SHARINGSOCKET : Usuário: " + file_sharing_username);
                        main.appendMessage("CMD_SHARINGSOCKET : O arquivo está sendo aberto");
                        break;

                    case "CMD_SENDFILE":
                        main.appendMessage("CMD_SENDFILE : O cliente está enviando um arquivo ...");
                        /*
                         Format: CMD_SENDFILE [NomeArquivo] [Tamanho] [Destinatário] [Consignee]  from: Sender Format
                         Format: CMD_SENDFILE [NomeArquivo] [Tamanho] [Remetente] to Receiver Format
                         */
                        String file_name = st.nextToken();
                        String filesize = st.nextToken();
                        String sendto = st.nextToken();
                        String consignee = st.nextToken();
                        main.appendMessage("CMD_SENDFILE : De: " + consignee);
                        main.appendMessage("CMD_SENDFILE : Para: " + sendto);
                        /**
                         * Obter cliente Socket *
                         */
                        main.appendMessage("CMD_SENDFILE : pronto para conexões .. ");
                        Socket cSock = main.getClientFileSharingSocket(sendto); /* Remetente Socket  */
                        /*   Now Check if the consignee socket was exists.   */

                        if (cSock != null) { /* Exists   */

                            try {
                                main.appendMessage("CMD_SENDFILE : Já conectado ..! ");
                                /**
                                 * O primeiro é escrever o nome do arquivo ..*
                                 */
                                main.appendMessage("CMD_SENDFILE : enviando arquivo para o cliente ..");
                                DataOutputStream cDos = new DataOutputStream(cSock.getOutputStream());
                                cDos.writeUTF("CMD_SENDFILE " + file_name + " " + filesize + " " + consignee);
                                /**
                                 * O segundo é ler o conteúdo do arquivo   *
                                 */
                                InputStream input = socket.getInputStream();
                                OutputStream sendFile = cSock.getOutputStream();
                                byte[] buffer = new byte[BUFFER_SIZE];
                                int cnt;
                                while ((cnt = input.read(buffer)) > 0) {
                                    sendFile.write(buffer, 0, cnt);
                                }
                                sendFile.flush();
                                sendFile.close();
                                /**
                                 * Excluir a lista de clientes *
                                 */
                                main.removeClientFileSharing(sendto);
                                main.removeClientFileSharing(consignee);
                                main.appendMessage("CMD_SENDFILE : Arquivo enviado ao cliente ...");
                            } catch (IOException e) {
                                main.appendMessage("[CMD_SENDFILE]: " + e.getMessage());
                            }
                        } else { /*   Não existe, erro de retorno  */
                            /*   FORMATo: CMD_SENDFILEERROR  */

                            main.removeClientFileSharing(consignee);
                            main.appendMessage("CMD_SENDFILE : Cliente '" + sendto + "' não encontrado.!");
                            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                            dos.writeUTF("CMD_SENDFILEERROR " + "Cliente '" + sendto + "'não encontrado, o compartilhamento de arquivo será encerrado.");
                        }
                        break;

                    case "CMD_SENDFILERESPONSE":
                        /*
                         Format: CMD_SENDFILERESPONSE [Usuario] [Mensagem]
                         */
                        String receiver = st.nextToken(); // método para receber o nome de usuário do receptor
                        String rMsg = ""; // método para receber mensagem de erro
                        main.appendMessage("[CMD_SENDFILERESPONSE]: Usuário: " + receiver);
                        while (st.hasMoreTokens()) {
                            rMsg = rMsg + " " + st.nextToken();
                        }
                        try {
                            Socket rSock = (Socket) main.getClientFileSharingSocket(receiver);
                            DataOutputStream rDos = new DataOutputStream(rSock.getOutputStream());
                            rDos.writeUTF("CMD_SENDFILERESPONSE" + " " + receiver + " " + rMsg);
                        } catch (IOException e) {
                            main.appendMessage("[CMD_SENDFILERESPONSE]: " + e.getMessage());
                        }
                        break;

                    case "CMD_SEND_FILE_XD":  // Format: CMD_SEND_FILE_XD [Remetente] [Destinatário]                        
                        try {
                            String send_sender = st.nextToken();
                            String send_receiver = st.nextToken();
                            String send_filename = st.nextToken();
                            main.appendMessage("[CMD_SEND_FILE_XD]: Host: " + send_sender);
                            this.createConnection(send_receiver, send_sender, send_filename);
                        } catch (Exception e) {
                            main.appendMessage("[CMD_SEND_FILE_XD]: " + e.getLocalizedMessage());
                        }
                        break;

                    case "CMD_SEND_FILE_ERROR":  // Format:  CMD_SEND_FILE_ERROR [Destinatário] [Mensagem]
                        String eReceiver = st.nextToken();
                        String eMsg = "";
                        while (st.hasMoreTokens()) {
                            eMsg = eMsg + " " + st.nextToken();
                        }
                        try {
                            /*  Enviar erro ao host de compartilhamento de arquivos */
                            Socket eSock = main.getClientFileSharingSocket(eReceiver);// método de obter o soquete do host de compartilhamento de arquivos para conexão
                            DataOutputStream eDos = new DataOutputStream(eSock.getOutputStream());
                            //  Format:  CMD_RECEIVE_FILE_ERROR [Message]
                            eDos.writeUTF("CMD_RECEIVE_FILE_ERROR " + eMsg);
                        } catch (IOException e) {
                            main.appendMessage("[CMD_RECEIVE_FILE_ERROR]: " + e.getMessage());
                        }
                        break;

                    case "CMD_SEND_FILE_ACCEPT": // Format:  CMD_SEND_FILE_ACCEPT [receiver] [Message]
                        String aReceiver = st.nextToken();
                        String aMsg = "";
                        while (st.hasMoreTokens()) {
                            aMsg = aMsg + " " + st.nextToken();
                        }
                        try {
                            /*  Send Error to the File Sharing host  */
                            Socket aSock = main.getClientFileSharingSocket(aReceiver); // get the file sharing host socket for connection
                            DataOutputStream aDos = new DataOutputStream(aSock.getOutputStream());
                            //  Format:  CMD_RECEIVE_FILE_ACCEPT [Message]
                            aDos.writeUTF("CMD_RECEIVE_FILE_ACCEPT " + aMsg);
                        } catch (IOException e) {
                            main.appendMessage("[CMD_RECEIVE_FILE_ERROR]: " + e.getMessage());
                        }
                        break;

                    default:
                        main.appendMessage("[CMDException]: Comando desconhecido"+ CMD);
                        break;
                }
            }
        } catch (IOException e) {
            /*   Esta é a função do cliente de bate-papo, remova, se existir.   */
            System.out.println(client);
            System.out.println("File Sharing: " + filesharing_username);
            main.removeFromTheList(client);
            if (filesharing_username != null) {
                main.removeClientFileSharing(filesharing_username);
            }
            main.appendMessage("[SocketThread]: Conexão do cliente fechada ..!");
        }
    }

}
