package mock;

import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.classes.Classes;

public enum EmptyCommands implements CommandAlias {
    ;

    @Override
    public Classes getCommandClasses() {
        return null;
    }
}
