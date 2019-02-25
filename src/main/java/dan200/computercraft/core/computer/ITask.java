/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import javax.annotation.Nonnull;

public interface ITask
{
    @Nonnull
    Computer getOwner();

    void execute();
}
