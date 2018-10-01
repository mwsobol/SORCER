package sorcer.service;

public class Name implements Identifiable {

        private static final long serialVersionUID = 1L;

        public String name;

        public Name() {
        }

        public Name(String name) {
            this.name = name;
        }

    @Override
    public Object getId() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }
}

