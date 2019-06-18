package sorcer.pml.provider.impl;

/**
 * @author Mike Sobolewski
 */

import sorcer.ent.operator;
import sorcer.pml.provider.Cylinder;
import sorcer.pml.provider.Sphere;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.out;
import static sorcer.co.operator.val;
import static sorcer.ent.operator.ent;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.*;


@SuppressWarnings("rawtypes")
public class Volume implements Sphere, Cylinder, Serializable {
	private static final long serialVersionUID = 1L;
	private final static Logger logger = LoggerFactory.getLogger(Volume.class
			.getName());
	private static double radius = 0.0, height = 0.0;
	 
	public Volume() {}
	
	public Volume(Context context) {}
	
	public Context getSphereSurface(Context context) throws RemoteException, ContextException {
		double radius = (Double) value(context, "sphere/radius");
		add(context,
			ent("sphere/surface", 4.0 * Math.PI * Math.pow(radius, 3)));
		return context;
	}

	public Context getSphereVolume(Context context) throws ContextException, RemoteException {
		double radius = (Double) value(context, "sphere/radius");
		add(context, ent("sphere/volume",
			(4.0 / 3.0) * Math.PI * Math.pow(radius, 3)));
		return context;
	}

	public Context getCylinderSurface(Context context) throws ContextException, RemoteException {
		double radius = (Double) value(context, "cylinder/radius");
		double height = (Double) value(context, "cylinder/height");
		add(context, ent("cylinder/surface",
				(2 * Math.PI * Math.pow(radius, 2))
						+ (2 * Math.PI * radius * height)));
		return context;
	}

	public Context getCylinderVolume(Context context) throws ContextException, RemoteException {
		double radius = (Double) value(context, "cylinder/radius");
		double height = (Double) value(context, "cylinder/height");
		add(context, ent("cylinder/volume",
				Math.PI * Math.pow(radius, 2) * height));
		return context;
	}

	@SuppressWarnings("unchecked")
	public static void main(String... args) throws ContextException, RemoteException {
		Volume v = new Volume();
		if (args.length == 2 && args[0].equals("cylinder")
				&& args[1].equals("input")) {

			Scanner scanner = new Scanner(System.in);
			try {
				while (scanner.hasNextLine()) {
					processLine(scanner.nextLine());
				}
			} finally {
				scanner.close();
			}
			Context context = context(val("cylinder/radius", radius),
					val("cylinder/height", height));
			out.println("cylinder volume: " + v.getCylinderVolume(context));
		} else {
			if (args[0].equals("cylinder")) {
				Context context = context(val("cylinder/radius", 2.0),
						val("cylinder/height", 3.0));
				out.println("cylinder volume: " + v.getCylinderVolume(context));
			} else if (args[0].equals("sphere")) {
				Context context = context(val("sphere/radius", 2.0));
				out.println("sphere volume: " + v.getSphereVolume(context));
			}
		}
	}
	
	private static void processLine(String line) {
		// use a second Scanner to compute the content of each line
		Scanner scanner = new Scanner(line);
		scanner.useDelimiter("=");
		if (scanner.hasNext()) {
			String key = scanner.next().trim();
			String value = scanner.next().trim();
			logger.info("key: " + key + " eval: " + value);
			if (key.equals("cylinder/radius"))
				radius = new Double(value);
			else if (key.equals("cylinder/height"))
				height = new Double(value);
		} else {
			logger.info("Empty or invalid line. Unable to compute.");
		}
		// no need to call scanner.close(), since the source is a String
	}

}
	
