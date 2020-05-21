
import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.JOptionPane;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author juaninha
 */
public class ClientThread implements Runnable{
    
    Socket socket;
    DataInputStream dis;
    DataOutputStream dos;
    MainForm main;
    StringTokenizer st;
    protected DecimalFormat df = new DecimalFormat("##,#00");
    
    public ClientThread(Socket socket, MainForm main){
        this.main = main;
        this.socket = socket;
        try {
            dis = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            main.appendMessage("[IOException]: "+ e.getMessage(), "Erro", Color.RED, Color.RED);
        }
    }


    @Override
    public void run() {
        try {
            while(!Thread.currentThread().isInterrupted()){
                String data = dis.readUTF();
                st = new StringTokenizer(data);
                /** Pega a mensagem do CMD **/
                String CMD = st.nextToken();
                switch(CMD){
                    case "CMD_MESSAGE":
                        SoundEffect.MessageReceive.play(); //  Toca Audio 
                        String msg = "";
                        String frm = st.nextToken();
                        while(st.hasMoreTokens()){
                            msg = msg +" "+ st.nextToken();
                        }
                        main.appendMessage(msg, frm, Color.MAGENTA, Color.BLUE);
                        break;
                        
                    case "CMD_ONLINE":
                        Vector online = new Vector();
                        while(st.hasMoreTokens()){
                            String list = st.nextToken();
                            if(!list.equalsIgnoreCase(main.username)){
                                online.add(list);
                            }
                        }
                        main.appendOnlineList(online);
                        break;
                    
                        
                    //  Esta função notificará o cliente que existe um arquivo para receber, Aceitar ou rejeitar o arquivo  
                    case "CMD_FILE_XD":  // Formato:  CMD_FILE_XD [origem] [destino] [nome_arquivo]
                        String sender = st.nextToken();
                        String receiver = st.nextToken();
                        String fname = st.nextToken();
                        int confirm = JOptionPane.showConfirmDialog(main, "De: "+sender+"\n Nome do arquivo : "+fname+"\n Deseja baixar esse arquivo?");
                        //SoundEffect.FileSharing.play(); //   Toca Audio
                        if(confirm == 0){ // O cliente aceita a solicitação e informa o remetente para enviar o arquivo

                            /* Escolha onde salvar o arquivo   */
                            main.openFolder();
                            try {
                                dos = new DataOutputStream(socket.getOutputStream());
                                // Format:  CMD_SEND_FILE_ACCEPT [Destinatario] [Mensagem]
                                String format = "CMD_SEND_FILE_ACCEPT "+sender+" Chấp nhận";
                                dos.writeUTF(format);
                                
                                /*  
Essa função criará um soquete de compartilhamento de arquivos para criar um processamento de entrada do arquivo e esse soquete será fechado automaticamente quando concluído.  */
                                Socket fSoc = new Socket(main.getMyHost(), main.getMyPort());
                                DataOutputStream fdos = new DataOutputStream(fSoc.getOutputStream());
                                fdos.writeUTF("CMD_SHARINGSOCKET "+ main.getMyUsername());
                                /*  Execute o Thread para isso */
                                new Thread(new ReceivingFileThread(fSoc, main)).start();
                            } catch (IOException e) {
                                System.out.println("[CMD_FILE_XD]: "+e.getMessage());
                            }
                        } else { // O cliente rejeita a solicitação e envia o resultado ao remetente
                            try {
                                dos = new DataOutputStream(socket.getOutputStream());
                                // Formato:  CMD_SEND_FILE_ERROR [ParaDestinatário] [Mensagem]
                                String format = "CMD_SEND_FILE_ERROR "+sender+" O usuário negou sua solicitação ou perdeu a conexão.! \"";
                                dos.writeUTF(format);
                            } catch (IOException e) {
                                System.out.println("[CMD_FILE_XD]: "+e.getMessage());
                            }
                        }                       
                        break;   
                        
                    default: 
                        main.appendMessage("[CMDException]: Comando desconhecido "+ CMD, "CMDException", Color.RED, Color.RED);
                    break;
                }
            }
        } catch(IOException e){
            main.appendMessage(" Perdeu a conexão com o cliente, tente novamente.! \"", "Erro", Color.RED, Color.RED);
        }
    }
}