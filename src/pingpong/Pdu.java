package pingpong;

/**
 *
 * @author Scriven Scot & Pastor Florent
 */
public class Pdu {
    
    /**
     * Enumération de tous les type de PDU pouvant être échangé tout au long 
     * du jeu (Jeu, Ping....)
     */
    enum Type {
        Jeu,
        Ping,
        Pong,
        Smash,
        Ace,
        Service,
        Jeton_Service,
        Abandon,
        Reprise,
        PouvezVousRepeter,
        Refus,
        Ack,
        Pose,
    }
    
    String adDest;
    String adExp;
    Type unType;
    int numeroPaquet;
    int idPartie;
    int maxPoints;
    int numeroPaquetAcquitte;
    PingPong.Etat pose;
    
    /**
     * Constructeur pour :
     * <ul>
     * <li>Jeu</li>
     * <li>Ping</li>
     * <li> Pong</li>
     * <li>Smash</li>
     * <li>Ace</li>
     * <li>Service</li>
     * <li>Jeton_Service</li>
     * <li>Abandon</li>
     * <li>Reprise</li>
     * <li>PouvezVousRepeter</li>
     * <li>Refus</li>
     * <li>Ack</li>
     * <li>Pose</li>
     * </ul>
     * @param numeroPaquet
     * @param adExp
     * @param adDest
     * @param unType
     * @param idPartie
     */
    Pdu(int numeroPaquet, String adExp, String adDest, Type unType, int idPartie){
        this.numeroPaquet = numeroPaquet;
        this.adExp = adExp;
        this.adDest = adDest;
        this.unType = unType;
        this.idPartie = idPartie;
    }

    public String getAdDest() {
        return adDest;
    }

    public String getAdExp() {
        return adExp;
    }

    public Type getUnType() {
        return unType;
    }

    public int getNumeroPaquetAcquitte() {
        return numeroPaquet;
    }

    public int getIdPartie() {
        return idPartie;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    /**
     * Permet de rajouter dans l'Objet PDU le maxPoints pour : 
     * <ul>
     * <li>Jeu</li>
     * <li>Ping</li>
     * <li>Pong</li>
     * <li>Smash</li>
     * <li>Ace</li>
     * <li>Service</li>
     * <li>Jeton_Service</li>
     * <li>Abandon</li>
     * <li>Reprise</li>
     * <li>PouvezVousRepeter</li>
     * </ul>
     * @param maxPoints 
     */
    public void setMaxPoints(int maxPoints) {
        this.maxPoints = maxPoints;
    }

    public int getDernierNumPaquet() {
        return numeroPaquetAcquitte;
    }

    /**
     * Permet de rajouter le dernier paquet reçu utilsé pour :
     * <ul>
     * <li>Ack</li>
     * </ul>
     * @param dernierNumPaquet 
     */
    public void setDernierNumPaquet(int dernierNumPaquet) {
        this.numeroPaquetAcquitte = dernierNumPaquet;
    }

    public PingPong.Etat getPose() {
        return pose;
    }
    
    /**
     * Permet de d'indiquer le prochain paquet, utilsé pour :
     * <ul>
     * <li>Pose</li>
     * </ul>
     * @param pose 
     */
    public void setPose(PingPong.Etat pose) {
        this.pose = pose;
    }
    
    
    
}
