package sorcer.service;

public interface Opservice extends Service {

    /**
     * Returns service multi-fidelities of this service.
     */
    public Fi getMultiFi();

    /**
     * Returns a morpher updating at runtime multi-fidelities of this service.
     */
    public Morpher getMorpher();
}
