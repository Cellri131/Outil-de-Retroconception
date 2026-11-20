<?php

class AnimalPhp {
    private string $nom;
    private int $age;

    public function __construct(string $nom, int $age) {
        $this->nom = $nom;
        $this->age = $age;
    }

    public function getNom(): string {
        return $this->nom;
    }

    public function getAge(): int {
        return $this->age;
    }
}
