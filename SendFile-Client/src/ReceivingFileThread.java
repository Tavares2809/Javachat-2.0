
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.StringTokenizer;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitorInputStream;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author juaninha
 */
public class ReceivingFileThread implements Runnable {
    
    protected Socket socket;
    protected DataInputStream dis;
    protected DataOutputStream dos;
    protected MainForm main;
    protected StringTokenizer st;
    protected DecimalFormat df = new DecimalFormat("##,#00");
    private final int BUFFER_SIZE = 100;
    
    public ReceivingFileThread(Socket soc, MainForm m){
        this.socket = soc;
        this.main = m;
        try {
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.out.println("[ReceivingFileThread]: " +e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            while(!Thread.currentThread().isInterrupted()){
                String data = dis.readUTF();
                st = new StringTokenizer(data);
                String CMD = st.nextToken();
                
                switch(CMD){
                    
                    //   Essa função manipulará o recebimento de um arquivo em um processo em segundo plano de outro usuário
                    case "CMD_SENDFILE":
                        String consignee = null;
                            try {
                                String filename = st.nextToken();
                                int filesize = Integer.parseInt(st.nextToken());
                                consignee = st.nextToken(); // Obtem o nome do destinatário
                                main.setMyTitle("Carregando arquivo ....");
                                System.out.println("Carregando arquivo ....");
                                System.out.println("De: "+ consignee);
                                String path = main.getMyDownloadFolder() + filename;                                
                                /*  Cria uma Stream   */
                                FileOutputStream fos = new FileOutputStream(path);
                                InputStream input = socket.getInputStream();                                
                                /*  Barra de progresso   */
                                ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(main, "Baixando arquivo, por favor aguarde", input);
                                /*  Buffer   */
                                BufferedInputStream bis = new BufferedInputStream(pmis);
                                /**  Cria um arquivo temporário **/
                                byte[] buffer = new byte[BUFFER_SIZE];
                                int count, percent = 0;
                                while((count = bis.read(buffer)) != -1){
                                    percent = percent + count;
                                    int p = (percent / filesize);
                                    main.setMyTitle("Baixando arquivo  "+ p +"%");
                                    fos.write(buffer, 0, count);
                                }
                                fos.flush();
                                fos.close();
                                main.setMyTitle("Você está logado como: " + main.getMyUsername());
                                JOptionPane.showMessageDialog(null, "O arquivo foi baixado para \n'"+ path +"'");
                                System.out.println("O arquivo foi salvo em :"+ path);
                            } catch (IOException e) {
                                /*
                                Reenviar mensagem de erro ao remetente
                                Formato: CMD_SENDFILERESPONSE [nomeusuario] [mensagem]
                                */
                                DataOutputStream eDos = new DataOutputStream(socket.getOutputStream());
                                eDos.writeUTF("CMD_SENDFILERESPONSE "+ consignee +"Conexão perdida, tente novamente!");
                                
                                System.out.println(e.getMessage());
                                main.setMyTitle("Você está logado como: " + main.getMyUsername());
                                JOptionPane.showMessageDialog(main, e.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
                                socket.close();
                            }
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("[ReceivingFileThread]: " +e.getMessage());
        }
    }
}

