class AnimalPython:
    def __init__(self, nom: str, age: int):
        self.nom = nom
        self.age = age

    def get_nom(self) -> str:
        return self.nom

    def get_age(self) -> int:
        return self.age
