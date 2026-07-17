import { apiGet, apiPost, apiPut, apiDelete } from "./client";

export interface BackendMeal {
  id: number;
  cook_id: number;
  name: string;
  description: string | null;
  price: number | string;
  category: string | null;
  cuisine_type: string | null;
  is_available: 0 | 1 | boolean;
  is_vegetarian: 0 | 1 | boolean;
  is_vegan: 0 | 1 | boolean;
  image_url: string | null;
  cook_name: string;
  kitchen_name: string | null;
  created_at: string;
}

export interface DbMeal {
  id: number;
  cookId: number;
  name: string;
  cook: string;
  price: number;
  category: string;
  available: boolean;
  isVegetarian: boolean;
  isVegan: boolean;
  imageUrl: string | null;
}

export interface MealPayload {
  cook_id: number;
  name: string;
  description?: string;
  price: number;
  category: string;
  is_available: boolean;
  is_vegetarian: boolean;
  is_vegan: boolean;
  image_url?: string;
}

export function mapBackendMeal(m: BackendMeal): DbMeal {
  return {
    id: m.id,
    cookId: m.cook_id,
    name: m.name,
    cook: m.kitchen_name || m.cook_name,
    price: Number(m.price) || 0,
    category: m.category || m.cuisine_type || "Other",
    available: m.is_available === 1 || m.is_available === true,
    isVegetarian: m.is_vegetarian === 1 || m.is_vegetarian === true,
    isVegan: m.is_vegan === 1 || m.is_vegan === true,
    imageUrl: m.image_url,
  };
}

export async function fetchMeals(): Promise<DbMeal[]> {
  const data = await apiGet<{ success: boolean; meals: BackendMeal[] }>("/admin/meals");
  return data.meals.map(mapBackendMeal);
}

export async function createMealApi(payload: MealPayload): Promise<DbMeal> {
  const data = await apiPost<{ success: boolean; message: string; meal: BackendMeal }>(
    "/admin/meals",
    payload
  );
  return mapBackendMeal(data.meal);
}

export async function updateMealApi(id: number, payload: Partial<MealPayload>): Promise<DbMeal> {
  const data = await apiPut<{ success: boolean; message: string; meal: BackendMeal }>(
    `/admin/meals/${id}`,
    payload
  );
  return mapBackendMeal(data.meal);
}

export async function deleteMealApi(id: number): Promise<void> {
  await apiDelete<{ success: boolean; message: string }>(`/admin/meals/${id}`);
}
