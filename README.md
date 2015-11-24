# TABD-Generador-BDs
Genera de manera random una BD de documentos en MongoDB y mapea la colección en una BD de grafos en Neo4j.

#Modo de uso
1. Importar los drivers de Neo4j y MongoDB.
2. El servidor de mongo debe estar corriendo en el localhost. (mongod -dbpath <<ingresar path donde almacenar las BD>>)
3. El tamaño, el path de la BD-grafo, el nombre de la BD documentos y nombre de la colección pueden ser editados en los datos de coneccion. Por defecto crea 10 elementos en la colección persons en la BD DBVecinos. Estos se mapean a /grafos que es una carpeta que contiene una instancia de la BD grafos mapeada, con las relaciones Vecino y Mascota.
