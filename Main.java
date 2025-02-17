public class Main {
    public static void main(String[] args) {

        MetasulfateSession interpreter = new MetasulfateSession(true);

        System.out.println(interpreter.eval(
                "SUM SUM 1 3 'x"));
    }
}
