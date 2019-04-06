import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;

import org.xml.sax.SAXException;

public class Crawler implements Serializable {

    private PrintWriter flujoSalida;
    private Map<String, Ocurrencias> diccionario;
    private ArrayList<File> FAT;
    private ArrayList<Puntuacion> salida;
    private Integer[] PalabrasFichero = new Integer[2000];
    private String KEY_THESAURO = "KEY_THESAURO";
    private String KEY_DICTIONARY = "KEY_DICTIONARY";
    private String KEY_FAT = "KEY_FAT";
    private String KEY_CONTADOR = "KEY_CONTADOR";
    private ArrayList<String> Thesauro;
    private static Crawler instance = null;

    private Crawler() {
        try {
            diccionario = new TreeMap();
            flujoSalida = new PrintWriter("C:\\Users\\Watakamaku\\Desktop\\prueba\\salida.txt");
            Thesauro = new ArrayList<>();
            salida = new ArrayList<>();
            BufferedReader flujoEntrada = new BufferedReader(new FileReader("Thesauro.txt"));
            String linea = null;
            while ((linea = flujoEntrada.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(linea, " ");
                while (st.hasMoreTokens()) {
                    Thesauro.add(st.nextToken());
                }
            }
            FAT = new ArrayList<>();
            for (int i = 0; i < 2000; i++) {
                PalabrasFichero[i] = 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Crawler getInstance() {
        if (instance == null) {
            instance = new Crawler();
        }
        return instance;
    }

    public boolean findThesauro(String word) {
        for (int i = 0; i < Thesauro.size(); i++) {
            if (Thesauro.get(i).equals(word)) {
                return true;
            }
        }
        return false;
    }

    public void parsePDF(File fichero) {
        try {
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            FileInputStream inputstream = new FileInputStream(fichero);
            ParseContext pcontext = new ParseContext();

            PDFParser pdfparser = new PDFParser();
            pdfparser.parse(inputstream, handler, metadata, pcontext);

            File f = new File("PDF.txt");
            FileOutputStream flujoPDF = new FileOutputStream(f);
            flujoPDF.write(handler.toString().getBytes());
            flujoPDF.close();

            FAT.add(f);

            WordCount(f);

        } catch (SAXException | TikaException | IOException e) {
            e.printStackTrace();
        }
    }

    public void aumentarPalabrasTotales(int posicion) {
        PalabrasFichero[posicion]++;
    }

    public Integer obtenerPosFAT(File fichero) {
        for (int i = 0; i < FAT.size(); i++) {
            if (FAT.get(i).getName().equals(fichero.getName())) {
                return i;
            }
        }
        return -1;
    }

    public void saveObject() {
        Hashtable h = new Hashtable();
        h.put(KEY_DICTIONARY, diccionario);
        h.put(KEY_THESAURO, Thesauro);
        h.put(KEY_FAT, FAT);
        h.put(KEY_CONTADOR, PalabrasFichero);
        try {
            FileOutputStream fos = new FileOutputStream("h.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(h);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public boolean loadObject() {
        try {
            FileInputStream fis = new FileInputStream("h.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            Hashtable h = (Hashtable) ois.readObject();
            diccionario = (Map<String, Ocurrencias>) h.get(KEY_DICTIONARY);
            Thesauro = (ArrayList<String>) h.get(KEY_THESAURO);
            FAT = (ArrayList<File>) h.get(KEY_FAT);
            PalabrasFichero = (Integer[]) h.get(KEY_CONTADOR);
            return true;
        } catch (FileNotFoundException ex) {
            return false;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }

    public void WordCount(File fichero) {
        try {
            BufferedReader flujoEntrada = new BufferedReader(new FileReader(fichero));
            String linea;

            while ((linea = flujoEntrada.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(linea, " ()[].,:;{}\"\'");
                while (st.hasMoreTokens()) {
                    String s = st.nextToken();
                    if (!findThesauro(s)) {
                        Object o = diccionario.get(s);
                        Integer posFT = obtenerPosFAT(fichero);
                        if (o == null) {
                            diccionario.put(s, new Ocurrencias(posFT));
                        } else {
                            Ocurrencias ocu = (Ocurrencias) o;
                            ocu.añadirOcurrencia(posFT);
                            diccionario.put(s, ocu);
                        }
                    }
                }
            }
            List claves = new ArrayList(diccionario.keySet());
            Collections.sort(claves);

            Iterator i = claves.iterator();

            while (i.hasNext()) {
                Object k = i.next();
                flujoSalida.write(k + " aparece un total de " + diccionario.get(k));
            }
            flujoSalida.flush();
            flujoSalida.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     *Método que devuelve la extensión de un archivo
     */
    private String obtenerExtension(File fichero) {
        String nombre = fichero.getName();
        int tamanoExtension = nombre.lastIndexOf(".");
        if (tamanoExtension == -1) {
            return "";
        }
        return nombre.substring(tamanoExtension);
    }

    public void ListIt(File fichero) {

        if (!fichero.exists()) {
            System.out.println("No se puede leer " + fichero);
            return;
        }

        if (fichero.isDirectory()) {
            File[] listaFicheros = fichero.listFiles();
            for (int i = 0; i < listaFicheros.length; i++) {
                ListIt(listaFicheros[i]);
            }
        } else if (obtenerExtension(fichero).equals(".txt") || obtenerExtension(fichero).equals(".java")) {
            FAT.add(fichero);
            WordCount(fichero);
        } else if (obtenerExtension(fichero).equals(".pdf")) {
            parsePDF(fichero);
        }
    }

    public void show() {
        List claves = new ArrayList(diccionario.keySet());
        Collections.sort(claves);

        System.out.println("********************************************ARCHIVOS********************************************");
        for (int i = 0; i < FAT.size(); i++) {
            System.out.println("El archivo con el nombre " + FAT.get(i).getName() + " se encuentra en la posición " + i + " y tiene en total " + PalabrasFichero[i]);
        }
        System.out.println("****************************************************************************************\n\n");
        Iterator i = claves.iterator();
        System.out.println("********************************************PALABRAS********************************************");
        System.out.println(claves.toString());

        while (i.hasNext()) {
            Object k = i.next();
            System.out.print("La Palabra: " + k + " ");
            Ocurrencias oc = diccionario.get(k);
            oc.show();
        }
        System.out.println("****************************************************************************************\n\n");
    }

    public void mostrarSalidaCorrecta() {
        ArrayList<Integer> salidaCorrecta = new ArrayList<>();
        Puntuacion mejor;
        int loops = salida.size();
        while(loops > 0) {
            mejor = new Puntuacion(-1, -1);
            for (int i = 0; i< salida.size(); i++) {
                if (salida.get(i).getPuntuacion() > mejor.getPuntuacion()) {
                    mejor = salida.get(i);
                }
            }
            salidaCorrecta.add(mejor.getNumArchivo());
            loops--;
            salida.remove(mejor);
        }
        for (int i = 0; i < salidaCorrecta.size(); i++) {
            System.out.println((i+1) + ".- " + FAT.get(salidaCorrecta.get(i)).getName());
        }
    }

    public void Buscar(String palabra) {
        List claves = new ArrayList(diccionario.keySet());

        boolean enc = false;

        for (String palabraDiccionario : diccionario.keySet()) {
            if (palabraDiccionario.equals(palabra)) {
                enc = true;
                Ocurrencias o = diccionario.get(palabraDiccionario);
                for (Integer numArchivo : o.getTree().keySet()) {
                    float puntuacion = (o.getTree().get(numArchivo)*100) /PalabrasFichero[numArchivo];
                    salida.add(new Puntuacion(puntuacion, numArchivo));
                }
                mostrarSalidaCorrecta();
            }
        }

        if (!enc) {
            System.out.println("No existe ninguna coincidencia entre las palabras.");
        }
    }

    public static void main(String[] Args) {
        Crawler c = Crawler.getInstance();
        if (!c.loadObject()) {
            c.ListIt(new File("C:\\Users\\Watakamaku\\Desktop\\prueba"));
            c.saveObject();
            c.show();
        } else {
            System.out.println("El diccionario ya existe");
            c.show();
        }
        System.out.println("Introduce la palabra que quieres buscar (Escribe Salir para dejar de buscar):\n ");
        String palabra = "";
        Scanner teclado = new Scanner(System.in);
        while (!palabra.equals("Salir")) {
            palabra = teclado.nextLine();
            if (!palabra.equals("Salir")) {
                if (palabra != null) {
                    c.Buscar(palabra);
                } else {
                    System.out.println("La palabra es nula");
                }
            } else {
                System.out.println("Cerrando buscador");
            }
        }
    }
}
