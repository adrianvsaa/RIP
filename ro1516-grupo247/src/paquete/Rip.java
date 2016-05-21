package paquete;

import java.util.Scanner;

public class Rip {
	public static void main(String[] args){
		System.out.println("Inserte la contrasena deseada, la contrasena puede tener un maximo de 16 caracteres");
		Scanner entrada = new Scanner(System.in);
		String contrasena = null;
		while(true){
			contrasena =  entrada.nextLine();
			if(contrasena.length()>16)
				break;
			System.out.println("La contrasena tiene mas de 16 caracteres");
		}
		entrada.close();
		Router a;
		if(args.length==1){
			if(args[0].trim().split(":").length==2)
				a = new Router(Integer.parseInt(args[0].trim().split(":")[1]), args[0].trim().split(":")[0], contrasena);
			else
				a = new Router(5512, args[0].trim(), contrasena);
		}
		else
			a = new Router(contrasena);
		a.start();
		
	}
}
