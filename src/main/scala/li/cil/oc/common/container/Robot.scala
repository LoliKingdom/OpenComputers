package li.cil.oc.common.container

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.client.gui.Icons
import li.cil.oc.common.tileentity
import li.cil.oc.util.SideTracker
import li.cil.oc.{api, common}
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}
import net.minecraft.inventory.IInventory

class Robot(playerInventory: InventoryPlayer, robot: tileentity.Robot) extends Player(playerInventory, robot) {
  val hasScreen = robot.components.exists {
    case Some(buffer: api.component.TextBuffer) => true
    case _ => false
  }
  private val withScreenHeight = 256
  private val noScreenHeight = 108
  val deltaY = if (hasScreen) 0 else withScreenHeight - noScreenHeight

  addSlotToContainer(170 + 0 * slotSize, 232 - deltaY, common.Slot.Tool)
  addSlotToContainer(170 + 1 * slotSize, 232 - deltaY, robot.containerSlotType(1), robot.containerSlotTier(1))
  addSlotToContainer(170 + 2 * slotSize, 232 - deltaY, robot.containerSlotType(2), robot.containerSlotTier(2))
  addSlotToContainer(170 + 3 * slotSize, 232 - deltaY, robot.containerSlotType(3), robot.containerSlotTier(3))

  for (i <- 0 to 3) {
    val y = 156 + i * slotSize - deltaY
    for (j <- 0 to 3) {
      val x = 170 + j * slotSize
      addSlotToContainer(new InventorySlot(this, otherInventory, inventorySlots.size, x, y))
    }
  }
  for (i <- 16 until 64) {
    addSlotToContainer(new InventorySlot(this, otherInventory, inventorySlots.size, -10000, -10000))
  }

  addPlayerInventorySlots(6, 174 - deltaY)

  // This factor is used to make the energy values transferable using
  // MCs 'progress bar' stuff, even though those internally send the
  // values as shorts over the net (for whatever reason).
  private val factor = 100

  private var lastSentBuffer = -1

  private var lastSentBufferSize = -1

  @SideOnly(Side.CLIENT)
  override def updateProgressBar(id: Int, value: Int) {
    super.updateProgressBar(id, value)
    if (id == 0) {
      robot.globalBuffer = value * factor
    }

    if (id == 1) {
      robot.globalBufferSize = value * factor
    }
  }

  override def detectAndSendChanges() {
    super.detectAndSendChanges()
    if (SideTracker.isServer) {
      val currentBuffer = robot.globalBuffer.toInt / factor
      if (currentBuffer != lastSentBuffer) {
        lastSentBuffer = currentBuffer
        sendProgressBarUpdate(0, lastSentBuffer)
      }

      val currentBufferSize = robot.globalBufferSize.toInt / factor
      if (currentBufferSize != lastSentBufferSize) {
        lastSentBufferSize = currentBufferSize
        sendProgressBarUpdate(1, lastSentBufferSize)
      }
    }
  }

  override def canInteractWith(player: EntityPlayer) =
    super.canInteractWith(player) && robot.canInteract(player.getCommandSenderName)

  class InventorySlot(container: Player, inventory: IInventory, index: Int, x: Int, y: Int) extends StaticComponentSlot(container, inventory, index, x, y, common.Slot.None, common.Tier.Any) {
    def isValid = robot.isInventorySlot(getSlotIndex)

    @SideOnly(Side.CLIENT)
    override def func_111238_b() = isValid && super.func_111238_b()

    override def getBackgroundIconIndex = {
      if (isValid) super.getBackgroundIconIndex
      else Icons.get(common.Tier.None)
    }

    override def getStack = {
      if (isValid) super.getStack
      else null
    }
  }

}