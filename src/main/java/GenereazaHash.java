import org.mindrot.jbcrypt.BCrypt;

public class GenereazaHash {

    public static void main(String[] args) {
        String parolaMeaSecreta = "test";
        String parolaHash = BCrypt.hashpw(parolaMeaSecreta, BCrypt.gensalt());

        System.out.println("Parola originala: " + parolaMeaSecreta);
        System.out.println("");
        System.out.println("--- COPIATI ACEST HASH ---");
        System.out.println(parolaHash);
        System.out.println("--------------------------");
    }
}