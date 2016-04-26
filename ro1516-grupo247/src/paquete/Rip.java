package paquete;

public class Rip {
	public static void main(String[] args){
		Router a;
		if(args.length==1)
			a = new Router(Integer.parseInt(args[0]));
		else
			a = new Router();
		a.start();
	}
}
