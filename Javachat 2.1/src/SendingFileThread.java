
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DecimalFormat;
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
public class SendingFileThread implements Runnable {
    
    protected Socket socket;
    private DataOutputStream dos;
    protected SendFile form;
    protected String file;
    protected String receiver;
    protected String sender;
    protected DecimalFormat df = new DecimalFormat("##,#00");
    private final int BUFFER_SIZE = 100;
    
    public SendingFileThread(Socket soc, String file, String receiver, String sender, SendFile frm){
        this.socket = soc;
        this.file = file;
        this.receiver = receiver;
        this.sender = sender;
        this.form = frm;
    }

    @Override
    public void run() {
        try {
            form.disableGUI(false);
            System.out.println("Enviar arquivo ..!");
            dos = new DataOutputStream(socket.getOutputStream());
            /** Escreve nome do usuario, destinatario e nome do arquivo **/
            File filename = new File(file);
            int len = (int) filename.length();
            int filesize = (int)Math.ceil(len / BUFFER_SIZE); // método de obter tamanhos de arquivo
            String clean_filename = filename.getName();
            dos.writeUTF("CMD_SENDFILE "+ clean_filename.replace(" ", "_") +" "+ filesize +" "+ receiver +" "+ sender);
            System.out.println("De: "+ sender);
            System.out.println("Para: "+ receiver);
            /** Create an stream **/
            InputStream input = new FileInputStream(filename);
            OutputStream output = socket.getOutputStream();
            /* Processos na tela */
 
            // Leia o arquivo
            BufferedInputStream bis = new BufferedInputStream(input);
            /** Crie um local para armazenar o arquivo **/
            byte[] buffer = new byte[BUFFER_SIZE];
            int count, percent = 0;
            while((count = bis.read(buffer)) > 0){
                percent = percent + count;
                int p = (percent / filesize);
               
                form.updateProgress(p);
                output.write(buffer, 0, count);
            }
            /* GUI AttachmentForm atualizada */
            form.setMyTitle("O arquivo foi enviado!");
            form.updateAttachment(false); // Atualizar anexo
            JOptionPane.showMessageDialog(form, "Arquivo enviado com sucesso.!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            form.closeThis();
            /* Fechar arquivo de envio */
            output.flush();
            output.close();
            System.out.println("O arquivo foi enviado ..!");
        } catch (IOException e) {
            form.updateAttachment(false); //  Anexo atualizado
            System.out.println("[EnviarArquivo]: "+ e.getMessage());
        }
    }
}