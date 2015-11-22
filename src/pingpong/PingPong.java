/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pingpong;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Scot Scriven & Pastor Florent
 */
public class PingPong {

    public int NOMBRE_SERVICES = 5;
    
    enum Mode{
        pasJeuEnCours,
        jeuEnCoursDefinition,
        serviceApprete,
        serviceAttendu,
        tire,
        recu,
        echangeGagne,
        echangePerdu,
        partiePerdue,
        partieGagnee,
        bug
    }
    
    public class Etat{
        Mode modeactuel; //Le mode actuel dans lequel ce système est
        int serviceRestant; //Le nombre de services restant à ce joueur avant de passer la main
        int nbPointJoueur; //Le nombre de point qu'a ce joueur
        int nbPointAdversaire; //le nombre de point qu'a l'adversaire
        int scoreMaximal; //Quand l'un des joueur atteint ce nombre, la partie se termine
        int idPartie; //L'ID de la partie en cours

        public Etat() {
            modeactuel = Mode.pasJeuEnCours; //Le mode actuel dans lequel ce système est
            serviceRestant = 0; //Le nombre de services restant à ce joueur avant de passer la main
            nbPointJoueur = 0; //Le nombre de point qu'a ce joueur
            nbPointAdversaire = 0; //le nombre de point qu'a l'adversaire
            scoreMaximal = -1; //Quand l'un des joueur atteint ce nombre, la partie se termine
            idPartie = -1; //L'ID de la partie en cours
        }
        
        public Etat(Etat e){
            modeactuel = e.modeactuel;
            serviceRestant = e.serviceRestant;
            nbPointJoueur = e.nbPointJoueur; 
            nbPointAdversaire = e.nbPointAdversaire;
            scoreMaximal = e.scoreMaximal; 
            idPartie = e.idPartie; 
        }
        
        
    }

    /*
    Objets utiles pour le réseau
    */
    Socket requestSocket; //Le Socket utilisé si le joueur est en mode serveur
    ServerSocket providerSocket; //Le Socket utilisé si le joueur est en mode client
    Socket connection = null; //La connction entre les deux joueurs
    ObjectOutputStream out; //Le flux de données sortantes
    ObjectInputStream in; //Le flux de données entrantes
    
    /*
    Objets utiles pour le jeu
    */
    Etat etat; //L'état actuel du jeu
    int joueur; //Utiles pour l'écriture à l'écran
    int numeroPaquetAEnvoyer; //Le prochain paquet à envoyer
    Pdu dernierPduAttenteAck; //Le numéro du dernier paquet à acquitter envoyé
    Pdu dernierPduEnvoye; //Le dernier Pdu envoye.
    String adresseDest; //L'adresse de l'adversaire
    String adresseExp; //L'adresse du joueur local
    
    int nbAckFaux; //Le nombre de Ack reçu qui ne correspond pas au denier paquet envoyé. Si jamais il est superieur à trois, on envoie un paquet reprise
    int nbPaquetIllogiques; //Le nombres de paquets illogique dans l'état du système reçus depuis le dernier paquets logique
    Etat dernierEtatPose; //L'état du système lorsque le dernier pose a circulé sur le réseau
    int nbCoupTireDepuisService;
    
    PingPong() {
        etat = new Etat();
        numeroPaquetAEnvoyer = 0;
    }

    /**
     * Permet de choisir si l'on veut être client ou bien être serveur
     * @param appli
     * @throws IOException 
     */
    void choixMode(PingPong appli) throws IOException{
        System.out.println("Appuyer sur x à tout moment pour vous connecter, ou autre chose pour rester en mode serveur");
        Scanner reader = new Scanner(System.in);
        if (reader.next().charAt(0) == 'x') {
                // Mode client
                appli.runClient();
        }else{
            while(true){
                // Mode Serveur
                runModServer();
            }
        }
    }
    
    /**
     * Lancement en mode serveur
     * @throws IOException 
     */
    void runModServer() throws IOException {
        try {
            //1. creating a server socket
            providerSocket = new ServerSocket(2016, 10);
            //2. Wait for connection
            System.out.println("Waiting for connection");

            connection = providerSocket.accept();
            System.out.println("Connection received from " + connection.getInetAddress().getHostName());
            //3. get Input and Output streams
            out = new ObjectOutputStream(connection.getOutputStream());
            out.flush();
            in = new ObjectInputStream(connection.getInputStream());

            //Si il est serveur alors c'est le joueur 2
            joueur = 2;

            // Récuperation des adresses du serveur et du client
            adresseDest = connection.getInetAddress().getHostName();
            adresseExp = connection.getLocalAddress().getHostName();
            
            

            //4. The two parts communicate via the input and output streams
            do {
                try {
                    Pdu messagePdu = (Pdu) in.readObject();
                    traitement(messagePdu);

                } catch (ClassNotFoundException classnot) {
                    System.err.println("Data received in unknown format");
                }
            } while (true);

        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            //4: Closing connection
            try {
                in.close();
                out.close();
                providerSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * Lancement en mode Client
     */
    void runClient() {
        try {
            //1. creating a socket to connect to the server
            requestSocket = new Socket("localhost", 2016);
            System.out.println("Connected to localhost in port 2014");
            //2. get Input and Output streams
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());

            // Si il est client alors Joueur 1
            joueur = 1;

            // Récuperation des adresses du serveur et du client
            adresseDest = requestSocket.getInetAddress().getHostName();
            adresseExp = requestSocket.getLocalAddress().getHostName();
            
            // Généartion de maxPoints
            generateMaxPoint();
            
            //Envoie du premier paquet, commencement de la partie
            sendJeu();

            //3: Communicating with the server
            do {
                try {
                    Pdu messagePdu = (Pdu) in.readObject();
                    traitement(messagePdu);

                } catch (ClassNotFoundException classNot) {
                    System.err.println("data received in unknown format");
                }
            } while (true);
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            //4: Closing connection
            try {
                in.close();
                out.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

     /**
     * Envoie une réponse de connexion
     * @param numeroPaquet
     */
    void sendConnection() {
        String msg;
        msg = "Connection établie";
        try {
            out.writeObject(msg);
            out.flush();
            System.out.println("Joueur " + joueur + " > " + msg);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
    
    /*
     * Génère un PDU avec les informations de base.
     * @param type Le type de PDU à génèrer
     */
    Pdu pduFactory(Pdu.Type type) throws SystemeEtatInstableException{
        if(nbAckFaux != 0){
            System.out.println("Un ack n'a pas été reçu. Le système est instable et ne répondra pas.");
            throw new SystemeEtatInstableException();
        }
        
        Pdu retour = new Pdu(numeroPaquetAEnvoyer, this.adresseExp, this.adresseDest, type, this.etat.idPartie);
        numeroPaquetAEnvoyer++;
        
        return retour;
    }
    
    void send(Pdu pdu){
        
        String msg = pdu.getNumeroPaquetAcquitte() + " | " + pdu.getAdExp() + " | " + pdu.getAdDest() + " | " + pdu.getUnType() + " | " + pdu.getIdPartie();
        try {
            out.writeObject(pdu);
            out.flush();
            System.out.println("Joueur " + joueur + " > " + msg);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        //TODO Filtrer uniquement les PDU pour lesquels ont attend un ack
        dernierPduEnvoye = pdu;
    }
    
    /** 
     * Execute des action spéciales à éxectuer en entrant dans l'état
     * @param e 
     */
    void changerMode(Mode e){
       if(e == Mode.serviceApprete){
           nbCoupTireDepuisService = 0;
           if(Math.random() < 0.1 && this.etat.nbPointAdversaire - this.etat.nbPointAdversaire >= 3){
               sendAbandon();
           } else if (Math.random() < 0.1 ) {
               sendService();
           } else {
           sendCoupNormal();
           sendPose();
           }
       } else if (e == Mode.serviceAttendu){    
           nbCoupTireDepuisService = 0;
       } else if (e == Mode.recu){
           if(etat.serviceRestant <= 0 && nbCoupTireDepuisService == 0 && Math.random() < 0.1){
               sendAce();
           }
           if(Math.random() < 0.3){ sendSmash(); }
           else {sendCoupNormal(); }
           
           
       } else if (e == Mode.echangePerdu){
           this.etat.nbPointAdversaire++; // T_T
       } else if (e == Mode.echangeGagne){
           this.etat.nbPointJoueur++; // YAY!
       } else if (e == Mode.partiePerdue){
           System.out.println("La partie est perdue"); // T_T
       } else if (e == Mode.partieGagnee){
           System.out.println("La partie est gagnée"); // YAY!
           
       } else if (e == Mode.bug) {
           sendReprise();
       }
        
       this.etat.modeactuel = e;
       
       if(e == Mode.echangePerdu || e == Mode.echangeGagne){
           this.etat.serviceRestant--;
           if(this.etat.serviceRestant > 0){
               this.changerMode(Mode.serviceApprete);
           } else if(this.etat.serviceRestant == 0){
               this.sendJeton_Service();
               this.changerMode(Mode.serviceAttendu);
           } else if(this.etat.serviceRestant < 0){
               this.changerMode(Mode.serviceAttendu);
           }
       } 
    }
    
    private void sendService() {
        try {
            Pdu pdu = pduFactory(Pdu.Type.Service);
            send(pdu);
            
            this.etat.serviceRestant = 0;
        } catch (SystemeEtatInstableException ex) {
            Logger.getLogger(PingPong.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Envoie un PDU de type Jeu
     *
     * @param numeroPaquet
     * @param adDest
     * @param adExp
     */
    void sendJeu() {
        try {
            Pdu pdu = pduFactory(Pdu.Type.Jeu);
            pdu.setMaxPoints(this.etat.scoreMaximal);
            send(pdu);
            this.changerMode(Mode.serviceApprete);
            dernierPduAttenteAck = pdu;
        } catch (SystemeEtatInstableException ex) {
        }
        
    }

    /**
     * Envoie un PDU de type Ping
     *
     * @param numeroPaquet
     * @param adDest
     * @param adExp
     */
    void sendCoupNormal() {
        
        try {
            Pdu unPduCoupNormal = pduFactory(Pdu.Type.Pong);
            if(joueur == 1) pduFactory(Pdu.Type.Ping);        
            send(unPduCoupNormal);
            
            nbCoupTireDepuisService++;
            this.changerMode(Mode.tire);
        } catch (SystemeEtatInstableException ex) {
    }
        
    }

    /**
     * Envoie un PDU de type Smash
     *
     * @param numeroPaquet
     * @param adDest
     * @param adExp
     */
    void sendSmash() {
        try {
            Pdu pdu = pduFactory(Pdu.Type.Smash);
            send(pdu);
            
            this.changerMode(Mode.echangeGagne);
        } catch (SystemeEtatInstableException ex) {
            
        }
        
    }

    /**
     * Envoie un PDU de type Ace
     *
     * @param numeroPaquet
     * @param adDest
     * @param adExp
     */
    void sendAce() {
        try {
            Pdu pdu = pduFactory(Pdu.Type.Ace);
            send(pdu);
            this.changerMode(Mode.echangeGagne);
        } catch (SystemeEtatInstableException ex) {

        }
    }

    /**
     * Envoie PDU de type Jeton_Service
     *
     * @param numeroPaquet
     * @param adDest
     * @param adExp
     */
    void sendJeton_Service() {
        try {
            Pdu pdu = pduFactory(Pdu.Type.Jeton_Service);
            send(pdu);
        } catch (SystemeEtatInstableException ex) {
        }
    }

    /**
     * Envoie un PDU de type Abandon
     *
     * @param numeroPaquet
     * @param adDest
     * @param adExp
     */
    void sendAbandon() {
        try {
            Pdu pdu = pduFactory(Pdu.Type.Abandon);
            send(pdu);                 
            
            dernierPduAttenteAck = pdu;
            this.changerMode(Mode.partiePerdue);
        } catch (SystemeEtatInstableException ex) {
            
        }
        
    }

    /**
     * Envoie un PDU de type Reprise
     *
     * @param numeroPaquet
     * @param adDest
     * @param adExp
     */
    void sendReprise() {
        try {
            Pdu unPdu = pduFactory(Pdu.Type.Reprise);
            send(unPdu);
            
            traitementReprise();
        } catch (SystemeEtatInstableException ex) {
            Logger.getLogger(PingPong.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Envoie un PDU de type PouvezVousRepeter
     *
     * @param numeroPaquet
     * @param adDest
     * @param adExp
     */
    void sendPouvezVousRepeter() {
        try {
            Pdu pdu = pduFactory(Pdu.Type.PouvezVousRepeter);
            send(pdu);
        } catch (SystemeEtatInstableException ex) {
            Logger.getLogger(PingPong.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    

    /**
     * Envoie un PDU de type Ack
     *
     * @param numeroPaquet
     * @param adDest
     * @param adExp
     */
    void sendAck(int numPaquetAcknowledge) {
        Pdu unPdu;
        try {
            unPdu = pduFactory(Pdu.Type.Ack);
            unPdu.setDernierNumPaquet(numPaquetAcknowledge);
            send(unPdu);
        } catch (SystemeEtatInstableException ex) {
            Logger.getLogger(PingPong.class.getName()).log(Level.SEVERE, null, ex);
        }
        

    }

    /**
     * Envoie un PDU de type Pose
     *
     * @param numeroPaquet
     * @param adDest
     * @param adExp
     */
    void sendPose() {
        try {
            //Envoie un etat du système
            Pdu pdu = pduFactory(Pdu.Type.Pose);
            pdu.setPose(this.etat);
            send(pdu);
            
            dernierEtatPose = new Etat(this.etat);
        } catch (SystemeEtatInstableException ex) {
            
        }
    }
    
    /**
     * Génération aléatoire des points maximums pour gagner la partie
     * C'est le client qui génére le nombre
     * Ici un nombre entier entre 5 et 10
     */
    void generateMaxPoint(){
        this.etat.scoreMaximal = 5 + (int)(Math.random() * ((10 - 5) + 1));
    }

    /**
     * Un Dispatcher permettant tout d'abord de décomposer le Pdu puis d'appeler les 
     * fonctions spécifiques à chaque type de Pdu pour en faire un traitement
     * @param unPduRecu 
     */
    void traitement(Pdu unPduRecu){
        try {
            //On ack tous les paquets sauf les paquest Ack 
            if(unPduRecu.getUnType() == Pdu.Type.Ack){
                traitementAck(unPduRecu);
            }else if(unPduRecu.getUnType() == Pdu.Type.Jeu){
                traitementJeu(unPduRecu);
            }else if(unPduRecu.getUnType() == Pdu.Type.Ping){
                    traitementPing();
            }else if(unPduRecu.getUnType() == Pdu.Type.Pong){
                    traitementPong();
            }else if(unPduRecu.getUnType() == Pdu.Type.Abandon){
                    traitementAbandon(unPduRecu);
            }else if(unPduRecu.getUnType() == Pdu.Type.Ace){
                    traitementAce();
            }else if(unPduRecu.getUnType() == Pdu.Type.PouvezVousRepeter){
                    traitementPouvezVousRepeter();
            }else if(unPduRecu.getUnType() == Pdu.Type.Reprise){
                    traitementReprise();
            }else if(unPduRecu.getUnType() == Pdu.Type.Service || unPduRecu.getUnType() == Pdu.Type.Jeton_Service){
                    traitementService();
            }else if(unPduRecu.getUnType() == Pdu.Type.Smash){
                    traitementSmash();
            }else if(unPduRecu.getUnType() == Pdu.Type.Pose){
                traitementPose(unPduRecu);
            }
            
            nbPaquetIllogiques = 0;
        } catch (PaquetIllogiqueRecuException ex) {
            if(nbPaquetIllogiques >= 3){
                this.changerMode(Mode.bug);
            } else {
                nbPaquetIllogiques++;
                sendPouvezVousRepeter();
            }            
        }
    }
    
    /**
     * Lorsqu'un ack est reçu, la fonction doit vérifier que le numero de paquet envoyé 
     * avant ce ack doit correspondre au numero de paquet envoyé par le ack dans le champ 
     * "dernierPaquetRecu" 
     * Si pas vérifier alors il y a erreur et retourne faux
     * @return false || true
     */
    void traitementAck(Pdu unPduRecu){
        try{
            if(unPduRecu.getNumeroPaquetAcquitte() == this.dernierPduAttenteAck.numeroPaquet){
                throw new Exception("Acquittement non correct");
            }
            
           nbAckFaux = 0;
            
            if(dernierPduAttenteAck.getUnType() == Pdu.Type.Jeu && this.etat.modeactuel == Mode.pasJeuEnCours){
                this.changerMode(Mode.serviceApprete);
            }
            
            
        }catch(Exception ex){
            if(nbAckFaux >= 3){
                this.changerMode(Mode.bug);
            } else {
                sendPouvezVousRepeter();
                nbAckFaux++;
            }
        }
    }
    
    /**
     * Enregistrement d'un point, permettant de faire une reprise suite a une erreur
     * Ici on enregistre le maxPoints, le numero de paquet, les adresses de destination
     * et d'expedition mais aussi l'id de la partie
     */
    void traitementPose(Pdu pdu){
        Etat e = pdu.getPose();
        int tempScoreAdversaire = e.nbPointAdversaire;
        e.nbPointJoueur = e.nbPointAdversaire;
        e.nbPointAdversaire = tempScoreAdversaire;
        
        e.serviceRestant = this.etat.serviceRestant;
        
        dernierEtatPose = new Etat(e);
    }
    
    /**
     * Lorsque l'on reçois le pdu jeu, on renvoie juste un ack pour confirmer 
     * le début du jeu, et envoie le jeton de service pour que celui qui lance le jeu 
     * sert en premier
     */
    void traitementJeu(Pdu pdu) throws PaquetIllogiqueRecuException{
        try {
            if(this.etat.modeactuel == Mode.pasJeuEnCours && pdu.unType == Pdu.Type.Jeu){
                sendAck(pdu.numeroPaquet);
                this.changerMode(Mode.serviceAttendu);
            } else {
                throw new PaquetIllogiqueRecuException();
            }
        } catch (PaquetIllogiqueRecuException ex) {
        }
        
        System.out.println("Début du jeu");
    }
    
    /**
     * Permet de traiter un pong. On gere le smash, et aussi le renvoie d'un ping
     */
    void traitementPong() throws PaquetIllogiqueRecuException{
        if(this.etat.modeactuel == Mode.tire){
            this.changerMode(Mode.recu);
        } else {
            throw new PaquetIllogiqueRecuException();
        }
    }
    
    /**
     * Permet de traiter un pong. On gere le smash, et aussi le renvoie d'un pong
     */
    void traitementPing() throws PaquetIllogiqueRecuException{
        if(this.etat.modeactuel == Mode.serviceAttendu || this.etat.modeactuel == Mode.tire){
            this.changerMode(Mode.recu);
        } else {
            throw new PaquetIllogiqueRecuException();
        }
    }
    
    void traitementAbandon(Pdu pdu) throws PaquetIllogiqueRecuException{
        if(this.etat.modeactuel == Mode.serviceAttendu){
            this.sendAck(pdu.numeroPaquet);
            this.changerMode(Mode.partieGagnee);
        } else {
            throw new PaquetIllogiqueRecuException();
        }
    }
    
    void traitementAce() throws PaquetIllogiqueRecuException{
        if(etat.modeactuel == Mode.tire && etat.serviceRestant > 0 && nbCoupTireDepuisService == 1){
            this.changerMode(Mode.echangePerdu);
        } else {
            throw new PaquetIllogiqueRecuException();
        }
    }
    
    void traitementService() throws PaquetIllogiqueRecuException{
        if(etat.modeactuel == Mode.serviceAttendu && this.etat.serviceRestant < 0){ 
                this.etat.serviceRestant = NOMBRE_SERVICES;
                this.changerMode(Mode.serviceApprete);
        } else {
            throw new PaquetIllogiqueRecuException();
        }
        
    }
    
    void traitementPouvezVousRepeter(){
        send(dernierPduEnvoye);
    }
    
    void traitementReprise(){
        this.etat = this.dernierEtatPose;
        if(this.etat.serviceRestant > 0){
            this.changerMode(Mode.serviceApprete);
        } else {
            this.changerMode(Mode.serviceAttendu);
        }
        System.out.println("Reçois reprise");
    }
    
    void traitementSmash() throws PaquetIllogiqueRecuException{
        if(this.etat.modeactuel == Mode.tire){
            this.changerMode(Mode.echangePerdu);
        } else {
            throw new PaquetIllogiqueRecuException();
        }
    }
    
    /**
     * Programme principale
     * @param args
     * @throws IOException
     */
    public static void main(String args[]) throws IOException {
        PingPong appli = new PingPong();
        appli.choixMode(appli);

    }
}
