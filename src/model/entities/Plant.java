package model.entities;

import view.GridConverter;

public class Plant extends Entity implements Entity.GridAligned {
    private static final int DEFAULT_WIDTH = 70;
    private static final int DEFAULT_HEIGHT = 90;
    protected int cost;//阳关成本
    protected int cooldown;//冷却时间
    protected int row;//所在的行
    protected int column;//所在的列

    public Plant(int row, int column, int cost, int health) {
        this(GridConverter.colToX(column), GridConverter.rowToY(row), row, column, cost, health);
    }

    public Plant(int x, int y, int row, int column, int cost, int health) {
        super(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, health);
        this.cost = cost;
        this.cooldown = 0;
        this.row = row;
        this.column = column;
    }
    @Override
    public void update(){
        //更新冷却时间
        if(cooldown>0){
            cooldown--;
        }
    }
    //Getter方法

    public int getCost() {
        return cost;
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    // === 实现 GridAligned 统一接口 ===
    @Override
    public int getGridRow() { return row; }

    @Override
    public int getGridCol() { return column; }

    @Override
    public void setGridPosition(int row, int col) {
        this.row = row;
        this.column = col;
        this.x = GridConverter.colToX(col);
        this.y = GridConverter.rowToY(row);
    }

    // 移除冗余的自测 main，集成测试由上层统一触发
}