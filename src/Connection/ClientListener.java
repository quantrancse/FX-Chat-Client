package Connection;

import Controller.MessageController;
import Helper.FileDownloadHelper;
import Helper.FileHistoryHelper;
import Helper.MessageHistoryHelper;
import Model.Client;
import Model.FileInfo;
import Model.MessageModel;
import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientListener implements Runnable {

    private ServerSocket serverSocket;
    private static MessageController messageController;

    public ClientListener(int port, MessageController messageController)
    {
        System.out.println("Thread ClientListener start");
        try {
            this.serverSocket = new ServerSocket(port);
            ClientListener.messageController = messageController;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true)
        {
            try {
                Socket socket = this.serverSocket.accept();

                System.out.println("New client just connect to ClientListener");
                Client client = new Client(socket,new ObjectOutputStream(socket.getOutputStream()),new ObjectInputStream(socket.getInputStream()));

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        Signal request = Signal.getRequest(client.getObjectInputStream());
                        if(request != null)
                        {
                            switch (request.getAction()) {
                                case MESSAGE:
                                    handleMessageRequest((MessageModel) request.getData());
                                    break;
                                case FILE:
                                    handleFileRequest((FileInfo) request.getData());
                                    break;
                                default:
                                    System.out.println("A client call to unknown function !!");
                                    break;
                            }
                        }
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMessageRequest(MessageModel messageModel)
    {
        // If we are talking directly to the sender
        // we only need to refresh message + update chat history
        if(ClientListener.messageController.currentFriendAddress.getUser().getUsername().equals(messageModel.getSender().getUsername()))
        {
            ClientListener.messageController.refreshMessage(messageModel);

            //update chat history
            MessageHistoryHelper.writeMessageHistory(messageModel.getReceiver(), messageModel.getSender(), messageModel);
        }
        //else we don't need to refresh message , just only update chat history
        else
        {
            //update chat history
            MessageHistoryHelper.writeMessageHistory(messageModel.getReceiver(), messageModel.getSender(), messageModel);

            // Notice to the user
            ClientListener.messageController.notification(messageModel.getSender());
        }
    }

    private void handleFileRequest(FileInfo fileInfo)
    {
        // We have to refresh fileList if we are talking directly to sender
        if(ClientListener.messageController.currentFriendAddress.getUser().getUsername().equals(fileInfo.getSender().getUsername()))
        {
            ClientListener.messageController.refreshFile(fileInfo.getFilename(),FileDownloadHelper.absolutePath.concat(fileInfo.getFilename()));

            // Save file into directory
            FileDownloadHelper.storeFile(fileInfo);
            // Write file history
            FileHistoryHelper.writeFileHistory(fileInfo.getReceiver(),fileInfo.getSender(),fileInfo);
        }
        // else we just need store the file and write to file history
        else
        {
            // Save file into directory
            FileDownloadHelper.storeFile(fileInfo);
            // Write file history
            FileHistoryHelper.writeFileHistory(fileInfo.getReceiver(),fileInfo.getSender(),fileInfo);
            // Notice to the user
            ClientListener.messageController.notification(fileInfo.getSender());
        }
    }
}